package be.kbc.eap.nexus.internal.group;

import be.kbc.eap.nexus.CondaFacet;
import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.internal.CondaFacetUtils;
import be.kbc.eap.nexus.internal.Constants;
import be.kbc.eap.nexus.repodata.CondaRepoDataMerger;
import com.google.common.collect.Maps;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.group.GroupFacetImpl;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.AssetEvent;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;


@Named
@Facet.Exposed
public class CondaGroupFacet
        extends GroupFacetImpl {

    private final CondaRepoDataMerger repoDataMerger;

    private CondaFacet condaFacet;


    @Inject
    public CondaGroupFacet(RepositoryManager repositoryManager,
                           ConstraintViolationFactory constraintViolationFactory,
                           @Named(GroupType.NAME) Type groupType) {
        super(repositoryManager, constraintViolationFactory, groupType);
        this.repoDataMerger = new CondaRepoDataMerger();
    }

    @Override
    protected void doInit(final Configuration configuration) throws Exception {
        super.doInit(configuration);
        this.condaFacet = facet(CondaFacet.class);
    }

    /**
     * Fetches cached content if exists, or {@code null}.
     */
    @Nullable
    public Content getCached(final CondaPath condaPath) throws IOException
    {
        checkMergeHandled(condaPath);
        Content content = condaFacet.get(condaPath);
        if (condaPath.isHash()) {
            return content; // hashes are recalculated whenever metadata is merged, so they're always fresh
        }
        boolean stale = isStale(content);
        if(stale) {
            log.info("Content is stale");
        }
        else {
            log.info("Content is not stale");
        }
        return !stale ? content : null;
    }

    /**
     * Merges and caches and returns the merged metadata. Returns {@code null} if no usable response was in passed in
     * map.
     */
    @Nullable
    public Content mergeAndCache(final CondaPath condaPath,
                                 final Map<Repository, Response> responses) throws IOException
    {

        log.info("Merge and cache for " + condaPath.getPath());

        checkMergeHandled(condaPath);
        // we do not cache subordinates/hashes, they are created as side-effect of cache
        checkArgument(!condaPath.isSubordinate(), "Only main content handled, not hash or signature: %s", condaPath);
        LinkedHashMap<Repository, Content> contents = Maps.newLinkedHashMap();
        for (Map.Entry<Repository, Response> entry : responses.entrySet()) {
            if (entry.getValue().getStatus().getCode() == HttpStatus.OK) {
                Response response = entry.getValue();
                if (response.getPayload() instanceof Content) {
                    contents.put(entry.getKey(), (Content) response.getPayload());
                }
            }
        }

        if (contents.isEmpty()) {
            log.trace("No 200 OK responses to merge");
            return null;
        }
        final Path path = Files.createTempFile("group-merged-content", "tmp");
        Content content = null;
        try {
            if (condaFacet.getCondaPathParser().isRepodata(condaPath)) {

                List<InputStream> streams = contents.values().stream().map(c -> {
                    try {
                        return c.openInputStream();
                    } catch (IOException e) {

                    }
                    return null;
                }).collect(Collectors.toList());
                String result = repoDataMerger.mergeRepoDataFiles(streams);
                content = CondaFacetUtils.createTempContent(path, ContentTypes.APPLICATION_JSON,
                        (OutputStream outputStream) -> {
                            OutputStreamWriter bw = new OutputStreamWriter(outputStream);
                            log.info("Write " + result.length() + " chars to outputstream");
                            int idx = result.indexOf("olefile-0.45.1-py27_0.tar.bz2");
                            if(idx>0) {
                                log.info("olefile 0.45.1: " + result.substring(idx, idx+2000));
                            }
                            bw.write(result);
                            bw.flush();
                        }
                );

            }

            if (content == null) {
                log.trace("No content resulted out of merge");
                return null;
            }
            log.trace("Caching merged content");
            return cache(condaPath, content);
        }
        finally {
            Files.delete(path);
        }
    }

    /**
     * Verifies that merge is handled.
     */
    private void checkMergeHandled(final CondaPath condaPath) {
        checkArgument(
                condaPath.getFileName().endsWith(Constants.REPODATA_JSON),
                "Not handled by CondaGroupFacet merge: %s",
                condaPath
        );
    }

    /**
     * Caches the merged content and it's Maven2 format required sha1/md5 hashes along.
     */
    private Content cache(final CondaPath condaPath, final Content content) throws IOException {

        return CondaFacetUtils.putWithHashes(condaFacet, condaPath, maintainCacheInfo(content));
    }

    /**
     * Evicts the cached content and it's Maven2 format required sha1/md5 hashes along.
     */
    private void evictCache(final CondaPath condaPath) throws IOException {
        CondaFacetUtils.deleteWithHashes(condaFacet, condaPath);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void on(final AssetEvent event) {
        // only make DB changes on the originating node, as orient will also replicate those for us
        if (event.isLocal() && member(event.getRepositoryName()) && event.getComponentId() == null) {
            final String path = event.getAsset().name();
            final CondaPath mavenPath = condaFacet.getCondaPathParser().parsePath(path);
            // group deletes path + path.hashes, but it should do only on content change in member
            if (!mavenPath.isHash()) {
                UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
                try {
                    evictCache(mavenPath);
                }
                catch (IOException e) {
                    log.warn("Could not evict merged content from {} cache at {}", getRepository().getName(),
                            mavenPath.getPath(), e);
                }
                finally {
                    UnitOfWork.end();
                }
            }
        }
    }

}

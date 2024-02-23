package be.kbc.eap.nexus.internal.group;

import be.kbc.eap.nexus.CondaFacet;
import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.internal.CondaFacetUtils;
import be.kbc.eap.nexus.internal.Constants;
import be.kbc.eap.nexus.internal.ZstdUtils;
import be.kbc.eap.nexus.repodata.CondaRepoDataMerger;
import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.google.common.collect.Maps;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
                           @Named(GroupType.NAME) Type groupType,
                           RepositoryCacheInvalidationService repositoryCacheInvalidationService) {
        super(repositoryManager, constraintViolationFactory, groupType, repositoryCacheInvalidationService);
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
    public Content getCached(final CondaPath condaPath) throws IOException {
        checkMergeHandled(condaPath);
        Content content = condaFacet.get(condaPath);
        if (condaPath.isHash()) {
            return content; // hashes are recalculated whenever metadata is merged, so they're always fresh
        }
        boolean stale = isStale(content);
        if (stale) {
            log.debug("Content is stale");
        } else {
            log.debug("Content is not stale");
        }
        return !stale ? content : null;
    }

    /**
     * Merges and caches and returns the merged metadata. Returns {@code null} if no usable response was in passed in
     * map.
     */
    @Nullable
    public Content mergeAndCache(final CondaPath condaPath, final Map<Repository, Response> responses) throws IOException {

        log.debug("mergeAndCache for " + condaPath.getPath());
        log.debug("Map<Repository, Response> responses \n");
        responses.forEach((key, value) -> log.debug(key + " " + value));

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
                    } catch (IOException ignored) {

                    }
                    return null;
                }).collect(Collectors.toList());

                if (streams.size() == 1) {
                    content = CondaFacetUtils.createTempContent(path, ContentTypes.APPLICATION_JSON,
                            (OutputStream outputStream) -> {
                                try (OutputStreamWriter bw = new OutputStreamWriter(outputStream);
                                     InputStreamReader isr = new InputStreamReader(streams.get(0))) {
                                    char[] buf = new char[8192];
                                    int length;
                                    while ((length = isr.read(buf)) > 0) {
                                        bw.write(buf, 0, length);
                                    }
                                    bw.flush();
                                } catch (Exception e) {
                                    log.error(e.getMessage());
                                }
                            }
                    );
                } else {
                    String result = repoDataMerger.mergeRepoDataFiles(streams);
                    content = CondaFacetUtils.createTempContent(path, ContentTypes.APPLICATION_JSON,
                            (OutputStream outputStream) -> {
                                try (OutputStreamWriter bw = new OutputStreamWriter(outputStream)) {
                                    log.debug("Write " + result.length() + " chars to outputstream");
                                    bw.write(result);
                                    bw.flush();
                                } catch (Exception e) {
                                    log.error(e.getMessage());
                                }
                            }
                    );
                }

                streams.stream().map(s -> {
                    try {
                        s.close();
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                    return s;
                });

            } else if(condaFacet.getCondaPathParser().isRepodataZst(condaPath)) {
                // Implementation for .zst files

                log.debug("Handling repodata.zst");

                List<InputStream> streams = contents.values().stream().map(c -> {
                    try {
                        return c.openInputStream();
                    } catch (IOException ignored) {

                    }
                    return null;
                }).collect(Collectors.toList());

                if(streams.size() == 1) {
                    // for one stream we don't need to decompress and compress
                    log.debug("Only one stream so returning the file directly without decompressing");
                    content = CondaFacetUtils.createTempContent(path, "application/octet-stream",
                            (OutputStream outputStream) -> {
                                try (BufferedOutputStream bw = new BufferedOutputStream(outputStream);
                                     BufferedInputStream isr = new BufferedInputStream(streams.get(0))) {
                                    byte[] buf = new byte[8192];
                                    int length;
                                    while ((length = isr.read(buf)) > 0) {
                                        bw.write(buf, 0, length);
                                    }
                                    bw.flush();
                                } catch (Exception e) {
                                    log.error(e.getMessage());
                                }
                            }
                    );
                }
                else {
                    List<byte[]> decompressedData = responses.values().stream()
                            .filter(response -> response.getStatus().getCode() == HttpStatus.OK)
                            .map(response -> (Content) response.getPayload())
                            .filter(Objects::nonNull)
                            .map(contentPayload -> {
                                try (InputStream inputStream = contentPayload.openInputStream()) {
                                    return ZstdUtils.decompressZstdStream(inputStream);
                                } catch (IOException e) {
                                    log.error("Failed to decompress .zst content", e);
                                    return null;
                                }
                            })
                            .collect(Collectors.toList());


                    // convert to List<InputStream> for merging
                    List<InputStream> inputStreams = decompressedData.stream()
                            .map(ByteArrayInputStream::new)
                            .collect(Collectors.toList());

                    // Merge contents
                    String mergedJson = repoDataMerger.mergeRepoDataFiles(inputStreams);

                    content = CondaFacetUtils.createTempContent(path, "application/octet-stream",
                            (OutputStream outputStream) -> {
                                try (BufferedOutputStream bw = new BufferedOutputStream(outputStream)) {
                                    log.debug("Write " + mergedJson.length() + " chars to outputstream");
                                    // recompress JSON to .zst file
                                    byte[] recompressedData = ZstdUtils.compressZstdData(mergedJson.getBytes(StandardCharsets.UTF_8));
                                    bw.write(recompressedData);
                                    bw.flush();
                                } catch (Exception e) {
                                    log.error(e.getMessage());
                                }
                            });
                }
            }

            if (content == null) {
                log.trace("No content resulted out of merge");
                return null;
            }
            log.trace("Caching merged content");
            return cache(condaPath, content);
        } finally {
            Files.delete(path);
        }
    }


    /**
     * Verifies that merge is handled.
     */
    private void checkMergeHandled(final CondaPath condaPath) {
        log.debug("CheckMergeHandled for condaPath: " + condaPath);
        checkArgument(
                condaPath.getFileName().endsWith(Constants.REPODATA_JSON) ||
                condaPath.getFileName().endsWith(Constants.REPODATA_JSON_ZST),
                "Not handled by CondaGroupFacet merge: %s",
                condaPath
        );
    }

    /**
     * Caches the merged content and it's Maven2 format required sha1/md5 hashes along.
     */
    private Content cache(final CondaPath condaPath, final Content content) throws IOException {
        maintainCacheInfo(content.getAttributes());
        return CondaFacetUtils.putWithHashes(condaFacet, condaPath, content);
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
                } catch (IOException e) {
                    log.warn("Could not evict merged content from {} cache at {}", getRepository().getName(),
                            mavenPath.getPath(), e);
                } finally {
                    UnitOfWork.end();
                }
            }
        }
    }

}

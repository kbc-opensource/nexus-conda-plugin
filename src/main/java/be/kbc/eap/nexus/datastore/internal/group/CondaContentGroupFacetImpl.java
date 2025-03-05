package be.kbc.eap.nexus.datastore.internal.group;

import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.datastore.CondaContentFacet;
import be.kbc.eap.nexus.util.Constants;
import be.kbc.eap.nexus.CondaPath.HashType;
import be.kbc.eap.nexus.repodata.CondaRepoDataJsonMerger;
import be.kbc.eap.nexus.repodata.CondaRepoDataMerger;
import be.kbc.eap.nexus.repodata.CondaRepoDataZstMerger;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.group.GroupFacetImpl;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.thread.io.StreamCopier;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.*;
import java.util.*;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.io.ByteStreams.toByteArray;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.sonatype.nexus.repository.view.ContentTypes.TEXT_PLAIN;


@Named
@Facet.Exposed
public class CondaContentGroupFacetImpl
        extends GroupFacetImpl
        implements CondaGroupFacet {

    private final CondaRepoDataMerger repoDataMerger;
    private final CondaRepoDataJsonMerger repoDataJsonMerger;

    private final CondaRepoDataZstMerger repoDataZstMerger;


    private CondaContentFacet condaFacet;


    @Inject
    public CondaContentGroupFacetImpl(RepositoryManager repositoryManager,
                                      ConstraintViolationFactory constraintViolationFactory,
                                      @Named(GroupType.NAME) Type groupType,
                                      RepositoryCacheInvalidationService repositoryCacheInvalidationService) {
        super(repositoryManager, constraintViolationFactory, groupType, repositoryCacheInvalidationService);
        this.repoDataMerger = new CondaRepoDataMerger();
        repoDataZstMerger = new CondaRepoDataZstMerger();
        repoDataJsonMerger = new CondaRepoDataJsonMerger();
    }

    @Override
    protected void doInit(final Configuration configuration) throws Exception {
        super.doInit(configuration);
        this.condaFacet = facet(CondaContentFacet.class);
    }

    /**
     * Fetches cached content if exists, or {@code null}.
     */
    @Nullable
    public Content getCached(final CondaPath condaPath) throws IOException {

        checkMergeHandled(condaPath);

        final String path = prependIfMissing(condaPath.getPath(), "/");

        log.debug("Checking cache for {}", path);

        Optional<FluentAsset> fluentAsset = getRepository()
                .facet(ContentFacet.class)
                .assets()
                .path(path)
                .find();

        if (!fluentAsset.isPresent()) {
            log.debug("cache miss for {}", path);
            return null;
        }

        // hashes are recalculated whenever metadata is merged, so they're always fresh
        FluentAsset asset = fluentAsset.get();
        Content content = asset.download();
        if (content.getSize() == 0L) {
            log.debug("Corrupted repository metadata: {}, source: {}", path, getRepository().getName());
            // rebuilt metadata process will be triggered for a group repository
            return null;
        }

        if (condaPath.isHash()) {
            log.debug("Cache hit for hash {}", path);
            return new Content(content);
        }

        if (asset.isStale(cacheController)) {
            log.debug("Cache stale hit for {}", path);
            return null;
        }

        log.debug("Cache fresh hit for {}", path);
        return new Content(content);
    }

    /**
     * Merges and caches and returns the merged metadata. Returns {@code null} if no usable response was in passed in
     * map.
     */
    @Nullable
    public Content mergeAndCache(final CondaPath condaPath, final Map<Repository, Response> responses) throws IOException {
        return merge(
                condaPath,
                responses,
                this::createTempBlob,
                (tempBlob, contentType) -> {
                    log.trace("Caching merged content");
                    return cache(condaPath, tempBlob, contentType);
                }
        );
    }


    @Nullable
    private  <T extends Closeable> Content merge(final CondaPath condaPath, final Map<Repository, Response> responses, final Function<InputStream, T> streamFunction,
                          final CondaGroupFacet.ContentFunction<T> contentFunction)
            throws IOException {
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

        T data = null;

        try {
            String contentType = null;
            if (getRepository().facet(CondaContentFacet.class).getCondaPathParser().isRepodata(condaPath)) {
                data = merge(repoDataJsonMerger::merge, condaPath, contents, streamFunction);
                contentType = "application/json";
            }
            else if (getRepository().facet(CondaContentFacet.class).getCondaPathParser().isRepodataZst(condaPath)) {
                data = merge(repoDataZstMerger::merge, condaPath, contents, streamFunction);
                contentType = "application/octet-stream";
            }

            if (data == null) {
                log.trace("No content resulted out of merge");
                return null;
            }

            return contentFunction.apply(data, contentType);
        }
        finally {
            if (data != null) {
                data.close();
            }
        }
    }


    private Content cache(
            final CondaPath condaPath,
            final TempBlob tempBlob,
            final String contentType) throws IOException
    {
        try {
            Content content = new Content(getRepository().facet(CondaContentFacet.class)
                    .put(condaPath, new BlobPayload(tempBlob.getBlob(), contentType)));

            maintainCacheInfo(content.getAttributes());
            mayAddETag(content.getAttributes(), tempBlob.getHashes());

            for (Map.Entry<HashAlgorithm, HashCode> entry : tempBlob.getHashes().entrySet()) {
                getRepository().facet(CondaContentFacet.class)
                        .put(condaPath.hash(entry.getKey()), new StringPayload(entry.getValue().toString(), TEXT_PLAIN));
            }

            getRepository().facet(ContentFacet.class).assets().path(prependIfMissing(condaPath.getPath(), "/")).find()
                    .ifPresent(a -> {a.markAsCached(content); });

            return content;
        }
        catch (Exception e) {
            log.warn("Problem caching merged content {} : {}",
                    getRepository().getName(), condaPath.getPath(), e);
        }

        // Handle exception by forcing re-merge on next request and retrieving content from TempBlob
        getRepository().facet(ContentFacet.class).assets().path(prependIfMissing(condaPath.getPath(), "/")).find()
                .ifPresent(FluentAsset::markAsStale);

        try (InputStream in = tempBlob.get()) {
            // load bytes in memory before tempBlob vanishes; metadata shouldn't be too large
            // (don't include cache-related attributes since this content has not been cached)
            return new Content(new BytesPayload(toByteArray(in), contentType));
        }
    }


    private <T> T merge(
            final MetadataMerger merger,
            final CondaPath condaPath,
            final LinkedHashMap<Repository, Content> contents,
            final Function<InputStream, T> streamFunction)
    {
        return new StreamCopier<>(
                outputStream -> merger.merge(outputStream, condaPath, contents),
                streamFunction).read();
    }

    private TempBlob createTempBlob(final InputStream inputStream) {
        List<HashAlgorithm> hashAlgorithms = stream(HashType.values())
                .map(HashType::getHashAlgorithm)
                .collect(toList());

        return getRepository().facet(ContentFacet.class).blobs().ingest(inputStream, null, hashAlgorithms);
    }

    private void mayAddETag(
            final AttributesMap attributesMap,
            final Map<HashAlgorithm, HashCode> hashCodes)
    {
        if (attributesMap.contains(Content.CONTENT_ETAG)) {
            return;
        }
        HashCode sha1HashCode = hashCodes.get(HashAlgorithm.SHA1);
        if (sha1HashCode != null) {
            attributesMap.set(Content.CONTENT_ETAG, "{SHA1{" + sha1HashCode + "}}");
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

}

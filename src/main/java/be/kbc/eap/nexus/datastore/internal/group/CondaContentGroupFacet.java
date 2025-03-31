package be.kbc.eap.nexus.datastore.internal.group;

import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.datastore.CondaContentFacet;
import com.github.luben.zstd.Zstd;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.joda.time.DateTime;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUpdatedEvent;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.group.GroupFacetImpl;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
import java.util.function.Supplier;


@Named
@Facet.Exposed
public class CondaContentGroupFacet
        extends GroupFacetImpl
        implements EventAware.Asynchronous {

    private CondaContentFacet condaFacet;


    @Inject
    public CondaContentGroupFacet(RepositoryManager repositoryManager,
                                  ConstraintViolationFactory constraintViolationFactory,
                                  @Named(GroupType.NAME) Type groupType,
                                  RepositoryCacheInvalidationService repositoryCacheInvalidationService) {
        super(repositoryManager, constraintViolationFactory, groupType, repositoryCacheInvalidationService);
    }

    @Override
    protected void doInit(final Configuration configuration) throws Exception {
        super.doInit(configuration);
        this.condaFacet = facet(CondaContentFacet.class);
    }

    public boolean isStale(Content content, Map<Repository, Response> responses) {
        DateTime cacheModified = this.extractLastModified(content);
        if (cacheModified != null && !this.isStale(content)) {
            for(Map.Entry<Repository, Response> response : responses.entrySet()) {
                Response responseValue = response.getValue();
                if (responseValue.getStatus().getCode() == 200) {
                    Content responseContent = (Content)responseValue.getPayload();
                    if (responseContent != null) {
                        DateTime memberLastModified = responseContent.getAttributes().get("last_modified", DateTime.class);
                        if (memberLastModified == null || memberLastModified.isAfter(cacheModified)) {
                            Asset asset = content.getAttributes().get(Asset.class);
                            String name = asset != null ? asset.path() : "unknown";
                            this.log.debug("Found stale content while fetching {} from repository {}", name, response.getKey().getName());
                            return true;
                        }
                    }
                }
            }

            return false;
        } else {
            return true;
        }
    }

    public Content buildRepodataJson(Repository repository, CondaPath condaPath, Supplier<String> lazyMergeResult) {
        log.debug(repository.getName() + " - Storing repodata.json for {}", condaPath.getPath());
        final Content content = buildMergedRepodataJson(lazyMergeResult);
        if (content == null) {
            return null;
        }
        return this.condaFacet.putRepoData(condaPath, content);
    }

    public Content buildRepodataZst(Repository repository, CondaPath condaPath, Supplier<String> lazyMergeResult) {
        log.debug(repository.getName() + " - Storing repodata.zst for {}", condaPath.getPath());
        final Content content = buildMergedRepodataZst(lazyMergeResult);
        if (content == null) {
            return null;
        }
        return this.condaFacet.putRepoData(condaPath, content);
    }

    public Optional<Content> getRepodataFile(final Repository repository, CondaPath condaPath) {
        log.debug(repository.getName() + " - Fetching repodata file from disk for {}", condaPath.getPath());
        return this.condaFacet.getAsset(condaPath.getPath()).map(FluentAsset::download);
    }

    protected Content buildMergedRepodataJson(final Supplier<String> lazyMergeResult) {
        final String json = lazyMergeResult.get();
        if (json == null) {
            return null;
        }
        final Content content = new Content(new StringPayload(json, "application/json"));
        maintainCacheInfo(content.getAttributes());
        return content;
    }

    protected Content buildMergedRepodataZst(final Supplier<String> lazyMergeResult) {
        final String json = lazyMergeResult.get();
        if (json == null) {
            return null;
        }
        byte[] compressed = Zstd.compress(json.getBytes());

        final Content content = new Content(new BytesPayload(compressed, "application/octet-stream"));
        maintainCacheInfo(content.getAttributes());
        return content;
    }

    private DateTime extractLastModified(Content content) {
        DateTime lastModified;
        if (content != null && content.getAttributes().contains("last_modified")) {
            lastModified = (DateTime)content.getAttributes().get("last_modified", DateTime.class);
        } else {
            lastModified = null;
        }

        return lastModified;
    }



    @Subscribe
    @Guarded(by = { "STARTED" })
    @AllowConcurrentEvents
    public void on(final AssetCreatedEvent event) {
        log.trace("Asset created event received for {}", event.getAsset());
        this.maybeInvalidateCache((AssetEvent)event, false);
    }

    @Subscribe
    @Guarded(by = { "STARTED" })
    @AllowConcurrentEvents
    public void on(final AssetDeletedEvent event) {
        log.trace("Asset deleted event received for {}", event.getAsset());
        this.maybeInvalidateCache((AssetEvent)event, true);
    }


    @Subscribe
    @Guarded(by = { "STARTED" })
    @AllowConcurrentEvents
    public void on(final AssetUpdatedEvent event) {
        log.trace("Asset updated event received for {}", event.getAsset());
        this.maybeInvalidateCache((AssetEvent)event, false);
    }

    private void maybeInvalidateCache(final AssetEvent event, final boolean delete) {

        event.getRepository().ifPresent(repository -> {
            if (this.member(repository)) {
                CondaPath path = getRepository().facet(CondaContentFacet.class).getCondaPathParser().parsePath(event.getAsset().path());
                if(this.condaFacet.getCondaPathParser().isRepodataZst(path) || this.condaFacet.getCondaPathParser().isRepodata(path)) {
                    this.invalidateCache(event.getAsset(), delete);
                }
            }
        });

    }

    private void invalidateCache(final Asset asset, final boolean delete) {
        this.log.info("Invalidating cached content {} from {}", (Object)asset.path(), (Object)this.getRepository().getName());
        final Optional<FluentAsset> fluentAssetOpt = this.condaFacet.getAsset(asset.path());
        fluentAssetOpt.ifPresent(fluentAsset -> {
            if (delete) {
                fluentAsset.delete();
            }
            fluentAsset.markAsStale();
        });
    }
}

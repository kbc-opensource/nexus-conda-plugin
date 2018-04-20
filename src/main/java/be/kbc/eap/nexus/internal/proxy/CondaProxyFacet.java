package be.kbc.eap.nexus.internal.proxy;

import be.kbc.eap.nexus.CondaFacet;
import be.kbc.eap.nexus.CondaPath;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.transaction.UnitOfWork;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.IOException;

import static be.kbc.eap.nexus.internal.CondaFacetUtils.findAsset;

@Named
@Facet.Exposed
public class CondaProxyFacet
extends ProxyFacetSupport {
    @Nullable
    @Override
    protected Content getCachedContent(Context context) throws IOException {
        final CondaPath condaPath = context.getAttributes().require(CondaPath.class);
        log.info("Retrieve " + condaPath.getPath() + " from storage");
        return getCondaFacet().get(condaPath);
    }


    @Override
    protected Content store(Context context, Content content) throws IOException {
        final CondaPath condaPath = context.getAttributes().require(CondaPath.class);
        log.info("Store content for path " + condaPath.getPath());
        return getCondaFacet().put(condaPath, content);
    }

    @Override
    protected void indicateVerified(Context context, Content content, CacheInfo cacheInfo) throws IOException {
        final StorageTx tx = UnitOfWork.currentTx();
        final Bucket bucket = tx.findBucket(getRepository());
        final CondaPath condaPath = context.getAttributes().require(CondaPath.class);


        // by EntityId
        Asset asset = Content.findAsset(tx, bucket, content);
        if (asset == null) {
        // by format coordinates
            asset = findAsset(tx, bucket, condaPath);
        }
        if (asset == null) {
            log.debug("Attempting to set cache info for non-existent maven asset {}", condaPath.getPath());
            return;
        }

        log.debug("Updating cacheInfo of {} to {}", condaPath.getPath(), cacheInfo);
        CacheInfo.applyToAsset(asset, cacheInfo);
        tx.saveAsset(asset);

    }

    @Override
    protected String getUrl(@Nonnull Context context) {
        final CondaPath condaPath = context.getAttributes().require(CondaPath.class);
        return condaPath.getPath();
    }

    private CondaFacet getCondaFacet() {
        return getRepository().facet(CondaFacet.class);
    }

    @Nonnull
    @Override
    protected CacheController getCacheController(@Nonnull Context context) {
        final CondaPath condaPath = context.getAttributes().require(CondaPath.class);
        if(getCondaFacet().getCondaPathParser().isRepodata(condaPath)) {
            return cacheControllerHolder.getMetadataCacheController();
        }
        return cacheControllerHolder.getContentCacheController();
    }

    @Nullable
    @Override
    protected Content fetch(Context context, Content stale) throws IOException {
        final CondaPath condaPath = context.getAttributes().require(CondaPath.class);
        if(stale == null) {
            if(condaPath.getCoordinates() == null
                    && !getCondaFacet().getCondaPathParser().isRepodata(condaPath)) {
                return null;
            }
        }
        return super.fetch(context, stale);
    }


}

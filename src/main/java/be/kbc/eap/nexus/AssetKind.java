package be.kbc.eap.nexus;


import org.sonatype.nexus.repository.cache.CacheControllerHolder;

import javax.annotation.Nonnull;

public enum AssetKind {
    OTHER(CacheControllerHolder.CONTENT),
    REPODATA(CacheControllerHolder.METADATA),
    ARTIFACT(CacheControllerHolder.CONTENT);

    private final CacheControllerHolder.CacheType cacheType;

    AssetKind(CacheControllerHolder.CacheType cacheType) {
        this.cacheType = cacheType;
    }

    @Nonnull
    public CacheControllerHolder.CacheType getCacheType() {
        return this.cacheType;
    }
}
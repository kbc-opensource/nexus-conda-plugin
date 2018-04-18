package be.kbc.eap.nexus.internal.proxy;

import be.kbc.eap.nexus.CondaFacet;
import be.kbc.eap.nexus.CondaPath;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.IOException;

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
        //
    }

    @Override
    protected String getUrl(@Nonnull Context context) {
        final CondaPath condaPath = context.getAttributes().require(CondaPath.class);
        return condaPath.getPath();
    }

    private CondaFacet getCondaFacet() {
        return getRepository().facet(CondaFacet.class);
    }


}

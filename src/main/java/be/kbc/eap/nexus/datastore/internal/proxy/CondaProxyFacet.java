package be.kbc.eap.nexus.datastore.internal.proxy;

import be.kbc.eap.nexus.AssetKind;
import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.datastore.CondaContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentProxyFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.IOException;
import java.util.Optional;

@Named
public class CondaProxyFacet extends ContentProxyFacetSupport {

    @Nullable
    @Override
    protected Content getCachedContent(Context context) throws IOException {
        final CondaPath condaPath = context.getAttributes().require(CondaPath.class);

        log.info("Retrieve cached content for: " + condaPath.getPath() + " from storage in " + context.getRepository().getName());

        return getCondaFacet().getAsset(condaPath.getPath()).map(FluentAsset::download).orElse(null);
    }

    @Override
    protected Content store(Context context, Content content) throws IOException {
        final CondaPath condaPath = context.getAttributes().require(CondaPath.class);
        log.info("Store content for path " + condaPath.getPath() + " in " + context.getRepository().getName());
        return getCondaFacet().put(condaPath, content);
    }

    @Override
    protected String getUrl(@Nonnull Context context) {
        return context.getRequest().getPath().substring(1);
    }

    private CondaContentFacet getCondaFacet() {
        return getRepository().facet(CondaContentFacet.class);
    }


}

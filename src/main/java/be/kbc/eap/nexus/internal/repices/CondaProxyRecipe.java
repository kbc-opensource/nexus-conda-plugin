package be.kbc.eap.nexus.internal.repices;


import be.kbc.eap.nexus.CondaPathParser;
import be.kbc.eap.nexus.internal.CondaFacetImpl;
import be.kbc.eap.nexus.internal.CondaFormat;
import be.kbc.eap.nexus.internal.CondaSecurityFacet;
import be.kbc.eap.nexus.internal.matcher.CondaPathMatcher;
import be.kbc.eap.nexus.internal.proxy.CondaProxyFacet;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.RecipeSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.attributes.AttributesFacet;
import org.sonatype.nexus.repository.cache.NegativeCacheFacet;
import org.sonatype.nexus.repository.cache.NegativeCacheHandler;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.proxy.ProxyHandler;
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet;
import org.sonatype.nexus.repository.search.SearchFacet;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.storage.SingleAssetComponentMaintenance;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Route;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler;
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;
import org.sonatype.nexus.repository.view.matchers.AlwaysMatcher;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound;

@Singleton
@Named(CondaProxyRecipe.NAME)
public class CondaProxyRecipe
    extends RecipeSupport
{
    public static final String NAME = "conda-proxy";

    @Inject
    Provider<CondaSecurityFacet> securityFacet;

    @Inject
    Provider<ConfigurableViewFacet> viewFacet;

    @Inject
    Provider<HttpClientFacet> httpClientFacet;

    @Inject
    Provider<NegativeCacheFacet> negativeCacheFacet;

    @Inject
    Provider<CondaProxyFacet> proxyFacet;

    @Inject
    Provider<CondaFacetImpl> condaFacet;

    @Inject
    Provider<StorageFacet> storageFacet;

    @Inject
    Provider<AttributesFacet> attributesFacet;

    @Inject
    Provider<SingleAssetComponentMaintenance> componentMaintenance;

    @Inject
    Provider<SearchFacet> searchFacet;

    @Inject
    Provider<PurgeUnusedFacet> purgeUnusedFacet;

    @Inject
    ExceptionHandler exceptionHandler;

    @Inject
    TimingHandler timingHandler;

    @Inject
    SecurityHandler securityHandler;

    @Inject
    NegativeCacheHandler negativeCacheHandler;

    @Inject
    PartialFetchHandler partialFetchHandler;

    @Inject
    UnitOfWorkHandler unitOfWorkHandler;

    @Inject
    ProxyHandler proxyHandler;

    @Inject
    ConditionalRequestHandler conditionalRequestHandler;

    @Inject
    ContentHeadersHandler contentHeadersHandler;

    @Inject
    @Named(CondaFormat.NAME)
    CondaPathParser condaPathParser;



    @Inject
    protected CondaProxyRecipe(@Named(ProxyType.NAME) Type type, @Named(CondaFormat.NAME) Format format) {
        super(type, format);
    }

    @Override
    public void apply(@Nonnull Repository repository) throws Exception {
        repository.attach(securityFacet.get());
        repository.attach(configure(viewFacet.get()));
        repository.attach(httpClientFacet.get());
        repository.attach(negativeCacheFacet.get());
        repository.attach(proxyFacet.get());
        repository.attach(condaFacet.get());
        repository.attach(storageFacet.get());
        repository.attach(attributesFacet.get());
        repository.attach(componentMaintenance.get());
        repository.attach(searchFacet.get());
        repository.attach(purgeUnusedFacet.get());
    }

    private ViewFacet configure(final ConfigurableViewFacet facet) {
        Router.Builder builder = new Router.Builder();

        builder.route(new Route.Builder().matcher(new CondaPathMatcher(condaPathParser))
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(exceptionHandler)
                .handler(negativeCacheHandler)
                .handler(conditionalRequestHandler)
                .handler(partialFetchHandler)
                .handler(contentHeadersHandler)
                .handler(unitOfWorkHandler)
                .handler(proxyHandler).create());

        builder.defaultHandlers(notFound());
        facet.configure(builder.create());
        return facet;
    }
}

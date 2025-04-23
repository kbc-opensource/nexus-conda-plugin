package be.kbc.eap.nexus.datastore.internal.proxy;

import be.kbc.eap.nexus.CondaFormat;
import be.kbc.eap.nexus.datastore.CondaContentFacet;
import be.kbc.eap.nexus.datastore.internal.CondaRecipeSupport;
import be.kbc.eap.nexus.util.matcher.CondaPathMatcher;
import org.slf4j.Logger;
import org.sonatype.goodies.common.Loggers;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.proxy.ProxyHandler;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Route;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.nexus.repository.view.ViewFacet;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound;

@Named(CondaProxyRecipe.NAME)
@Singleton
@AvailabilityVersion(from = "1.0")
public class CondaProxyRecipe extends CondaRecipeSupport {

    public static final String NAME = "conda-proxy";

    private Provider<CondaContentFacet> contentFacet;
    private Provider<CondaProxyFacet> proxyFacet;
    private ProxyHandler proxyHandler;


    protected final Logger log = Loggers.getLogger(this);

    @Inject
    public CondaProxyRecipe(
            @Named(ProxyType.NAME) Type type,
            @Named(CondaFormat.NAME) Format format,
            final Provider<CondaContentFacet> contentFacet,
            final Provider<CondaProxyFacet> proxyFacet,
            final ProxyHandler proxyHandler) {
        super(type, format);
        this.contentFacet = contentFacet;
        this.proxyFacet = proxyFacet;
        this.proxyHandler = proxyHandler;
    }

    @Override
    public void apply(@Nonnull Repository repository) throws Exception {

        log.info("Attaching facets for proxy recipe");

        repository.attach(securityFacet.get());
        repository.attach(configure(viewFacet.get()));
        repository.attach(httpClientFacet.get());
        repository.attach(negativeCacheFacet.get());
        repository.attach(this.proxyFacet.get());
        repository.attach(this.contentFacet.get());
        repository.attach(searchFacet.get());
        repository.attach(browseFacet.get());
        repository.attach(purgeUnusedFacet.get());

        repository.attach(maintenanceFacet.get());
    }

    private ViewFacet configure(ConfigurableViewFacet facet) {
        Router.Builder builder = new Router.Builder();

        builder.route(new Route.Builder().matcher(new CondaPathMatcher(condaPathParser))
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(exceptionHandler)
                .handler(negativeCacheHandler)
                .handler(conditionalRequestHandler)
                .handler(partialFetchHandler)
                .handler(contentHeadersHandler)
                .handler(lastDownloadedHandler)
                //.handler(unitOfWorkHandler)
                .handler(proxyHandler).create());

        builder.defaultHandlers(notFound());

        facet.configure(builder.create());
        return facet;
    }
}

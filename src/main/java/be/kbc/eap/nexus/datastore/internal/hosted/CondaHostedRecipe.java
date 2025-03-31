package be.kbc.eap.nexus.datastore.internal.hosted;

import be.kbc.eap.nexus.CondaFormat;
import be.kbc.eap.nexus.datastore.CondaContentFacet;
import be.kbc.eap.nexus.datastore.internal.CondaRecipeSupport;
import be.kbc.eap.nexus.util.matcher.CondaPathMatcher;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Route;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound;

@Named(CondaHostedRecipe.NAME)
@Singleton
public class CondaHostedRecipe extends CondaRecipeSupport {

    public static final String NAME = "conda-hosted";

    CondaHostedHandler condaHostedHandler;

    private LastDownloadedHandler lastDownloadedHandler;

    private Provider<CondaContentFacet> contentFacet;

    private Provider<CondaHostedFacetImpl> condaHostedFacet;

    @Inject
    public CondaHostedRecipe(
            @Named(HostedType.NAME) Type type,
            @Named(CondaFormat.NAME) Format format,
            final Provider<CondaContentFacet> contentFacet,
            final Provider<CondaHostedFacetImpl> condaHostedFacet,
            final CondaHostedHandler condaHostedHandler,
            final LastDownloadedHandler lastDownloadedHandler) {
        super(type, format);
        this.contentFacet = contentFacet;
        this.condaHostedFacet = condaHostedFacet;
        this.condaHostedHandler = condaHostedHandler;
        this.lastDownloadedHandler = lastDownloadedHandler;
    }

    @Override
    public void apply(@Nonnull Repository repository) throws Exception {
        repository.attach(securityFacet.get());
        repository.attach(configure(viewFacet.get()));
        repository.attach(contentFacet.get());
        repository.attach(searchFacet.get());
        repository.attach(browseFacet.get());
        repository.attach(negativeCacheFacet.get());
        repository.attach(httpClientFacet.get());
        repository.attach(maintenanceFacet.get());
        repository.attach(condaHostedFacet.get());

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
                //.handler(unitOfWorkHandler)
                .handler(condaHostedHandler)
                .handler(lastDownloadedHandler)
                .create());

        builder.defaultHandlers(notFound());

        facet.configure(builder.create());
        return facet;
    }
}

package be.kbc.eap.nexus.internal.repices;

import be.kbc.eap.nexus.CondaPathParser;
import be.kbc.eap.nexus.internal.CondaFacetImpl;
import be.kbc.eap.nexus.internal.CondaFormat;
import be.kbc.eap.nexus.internal.CondaSecurityFacet;
import be.kbc.eap.nexus.internal.hosted.CondaHostedComponentMaintenanceFacet;
import be.kbc.eap.nexus.internal.hosted.CondaHostedHandler;
import be.kbc.eap.nexus.internal.matcher.CondaPathMatcher;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.RecipeSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.attributes.AttributesFacet;
import org.sonatype.nexus.repository.content.search.SearchFacet;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Route;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler;
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound;

@Named(CondaHostedRecipe.NAME)
@Singleton
public class CondaHostedRecipe
        extends RecipeSupport {
    public static final String NAME = "conda-hosted";

    @Inject
    Provider<CondaSecurityFacet> securityFacet;

    @Inject
    Provider<ConfigurableViewFacet> viewFacet;

    @Inject
    Provider<StorageFacet> storageFacet;

    @Inject
    Provider<AttributesFacet> attributesFacet;

    @Inject
    Provider<CondaFacetImpl> condaFacet;

//    @Inject
//    Provider<AptHostedComponentMaintenanceFacet> componentMaintenance;

    @Inject
    Provider<SearchFacet> searchFacet;

    @Inject
    ExceptionHandler exceptionHandler;

    @Inject
    TimingHandler timingHandler;

    @Inject
    SecurityHandler securityHandler;

    @Inject
    PartialFetchHandler partialFetchHandler;

    @Inject
    UnitOfWorkHandler unitOfWorkHandler;

    @Inject
    CondaHostedHandler hostedHandler;

    @Inject
    ConditionalRequestHandler conditionalRequestHandler;

    @Inject
    ContentHeadersHandler contentHeadersHandler;

    @Inject
    Provider<CondaHostedComponentMaintenanceFacet> componentMaintenanceFacet;

    @Inject
    @Named(CondaFormat.NAME)
    CondaPathParser condaPathParser;

    @Inject
    public CondaHostedRecipe(@Named(HostedType.NAME) Type type, @Named(CondaFormat.NAME) Format format) {
        super(type, format);
    }


    @Override
    public void apply(@Nonnull Repository repository) throws Exception {
        repository.attach(securityFacet.get());
        repository.attach(configure(viewFacet.get()));
        repository.attach(storageFacet.get());
        repository.attach(condaFacet.get());
        //repository.attach(condaSigningFacet.get());
        //repository.attach(snapshotFacet.get());
        repository.attach(attributesFacet.get());
        repository.attach(componentMaintenanceFacet.get());
        repository.attach(searchFacet.get());
    }

    private ViewFacet configure(final ConfigurableViewFacet facet) {
        Router.Builder builder = new Router.Builder();

        builder.route(new Route.Builder()
                .matcher(new CondaPathMatcher(condaPathParser))
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(exceptionHandler)
                .handler(conditionalRequestHandler)
                .handler(partialFetchHandler)
                .handler(contentHeadersHandler)
                .handler(unitOfWorkHandler)
                .handler(hostedHandler).create());

        builder.defaultHandlers(notFound());
        facet.configure(builder.create());
        return facet;
    }
}

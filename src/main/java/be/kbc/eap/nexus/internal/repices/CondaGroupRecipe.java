package be.kbc.eap.nexus.internal.repices;

import be.kbc.eap.nexus.CondaPathParser;
import be.kbc.eap.nexus.internal.CondaFacetImpl;
import be.kbc.eap.nexus.internal.CondaFormat;
import be.kbc.eap.nexus.internal.group.CondaGroupFacet;
import be.kbc.eap.nexus.internal.group.CondaMergingGroupHandler;
import be.kbc.eap.nexus.internal.CondaSecurityFacet;
import be.kbc.eap.nexus.internal.matcher.CondaPathMatcher;
import be.kbc.eap.nexus.internal.matcher.CondaRepoDataMatcher;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.RecipeSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.attributes.AttributesFacet;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Route;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler;
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler;
import org.sonatype.nexus.repository.view.handlers.HandlerContributor;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound;

@Named(CondaGroupRecipe.NAME)
@Singleton
class CondaGroupRecipe
        extends RecipeSupport {

    public static final String NAME = "conda-group";

    @Inject
    Provider<StorageFacet> storageFacet;

    @Inject
    Provider<CondaGroupFacet> groupFacet;

    @Inject
    Provider<CondaSecurityFacet> securityFacet;

    @Inject
    Provider<ConfigurableViewFacet> viewFacet;


    @Inject
    Provider<AttributesFacet> attributesFacet;


    @Inject
    ExceptionHandler exceptionHandler;

    @Inject
    Provider<CondaFacetImpl> condaFacet;

    @Inject
    TimingHandler timingHandler;

    @Inject
    SecurityHandler securityHandler;

    @Inject
    HandlerContributor handlerContributor;

    @Inject
    GroupHandler groupHandler;

    @Inject
    CondaMergingGroupHandler mergingGroupHandler;

    @Inject
    ConditionalRequestHandler conditionalRequestHandler;

    @Inject
    @Named(CondaFormat.NAME)
    CondaPathParser condaPathParser;

    @Inject
    public CondaGroupRecipe(@Named(GroupType.NAME) final Type type,
                     @Named(CondaFormat.NAME) final Format format)
    {
        super(type, format);
    }

    @Override
    public void apply(@Nonnull final Repository repository) throws Exception {
         repository.attach(storageFacet.get());
         repository.attach(securityFacet.get());
         repository.attach(attributesFacet.get());
         repository.attach(configure(viewFacet.get()));
         repository.attach(groupFacet.get());
         repository.attach(condaFacet.get());
    }

    @Inject
    ContentHeadersHandler contentHeadersHandler;

    @Inject
    UnitOfWorkHandler unitOfWorkHandler;

    @Inject
    PartialFetchHandler partialFetchHandler;

    Route.Builder newCondaPathRouteBuilder() {
        return new Route.Builder()
                .matcher(new CondaPathMatcher(condaPathParser))
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(exceptionHandler)
                .handler(handlerContributor)
                .handler(conditionalRequestHandler);
    }
    Route.Builder newMetadataRouteBuilder() {
        return new Route.Builder()
                .matcher(new CondaRepoDataMatcher(condaPathParser))
                .handler(timingHandler)
                .handler(securityHandler)
                .handler(exceptionHandler)
                .handler(conditionalRequestHandler);
    }

    /**
     * Configure {@link ViewFacet}.
     */
    private ViewFacet configure(final ConfigurableViewFacet viewFacet) {
        Router.Builder builder = new Router.Builder();

        //addBrowseUnsupportedRoute(builder);

        builder.route(newMetadataRouteBuilder()
                .handler(contentHeadersHandler)
                .handler(unitOfWorkHandler)
                .handler(mergingGroupHandler)
                .create());

        builder.route(newCondaPathRouteBuilder()
                .handler(partialFetchHandler)
                .handler(groupHandler)
                .create()
        );

        builder.defaultHandlers(notFound());

        viewFacet.configure(builder.create());

        return viewFacet;
    }

}

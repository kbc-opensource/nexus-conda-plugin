package be.kbc.eap.nexus.datastore.internal.group;

import be.kbc.eap.nexus.CondaFormat;
import be.kbc.eap.nexus.datastore.CondaContentFacet;
import be.kbc.eap.nexus.datastore.internal.CondaRecipeSupport;
import be.kbc.eap.nexus.util.matcher.CondaPathMatcher;
import be.kbc.eap.nexus.util.matcher.CondaRepoDataMatcher;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.types.GroupType;
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

@Named(CondaGroupRecipe.NAME)
@Singleton
public class CondaGroupRecipe extends CondaRecipeSupport {

    public static final String NAME = "conda-group";

    @Inject
    private Provider<CondaContentFacet> contentFacet;

    @Inject
    private GroupHandler groupHandler;

    @Inject
    private CondaGroupMergingHandler mergingGroupHandler;

    @Inject
    private Provider<CondaContentGroupFacetImpl> groupFacet;


    @Inject
    public CondaGroupRecipe(@Named(GroupType.NAME) Type type, @Named(CondaFormat.NAME) Format format) {
        super(type, format);
    }

    @Override
    public void apply(@Nonnull Repository repository) throws Exception {

        repository.attach(securityFacet.get());
        repository.attach(groupFacet.get());
        repository.attach(configure(viewFacet.get()));
        repository.attach(this.contentFacet.get());
        repository.attach(searchFacet.get());
        repository.attach(browseFacet.get());
        //repository.attach(httpClientFacet.get());
        repository.attach(negativeCacheFacet.get());
        repository.attach(purgeUnusedFacet.get());
        repository.attach(maintenanceFacet.get());
    }

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
                .handler(lastDownloadedHandler)
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
                //.handler(unitOfWorkHandler)
                .handler(mergingGroupHandler)
                .create());

        builder.route(newCondaPathRouteBuilder()
                .handler(partialFetchHandler)
                .handler(groupHandler)
                .handler(lastDownloadedHandler)
                .create()
        );

        builder.defaultHandlers(notFound());

        viewFacet.configure(builder.create());

        return viewFacet;
    }

}
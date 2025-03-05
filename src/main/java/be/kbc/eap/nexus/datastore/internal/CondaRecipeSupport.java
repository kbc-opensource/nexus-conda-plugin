package be.kbc.eap.nexus.datastore.internal;

import be.kbc.eap.nexus.CondaPathParser;
import be.kbc.eap.nexus.CondaFormat;
import be.kbc.eap.nexus.datastore.CondaSecurityFacet;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.RecipeSupport;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.NegativeCacheFacet;
import org.sonatype.nexus.repository.cache.NegativeCacheHandler;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.maintenance.LastAssetMaintenanceFacet;
import org.sonatype.nexus.repository.content.search.SearchFacet;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet;
import org.sonatype.nexus.repository.routing.RoutingRuleHandler;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.handlers.*;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

public abstract class CondaRecipeSupport extends RecipeSupport {

    @Inject
    protected Provider<BrowseFacet> browseFacet;

    @Inject
    protected Provider<CondaSecurityFacet> securityFacet;

    @Inject
    protected Provider<ConfigurableViewFacet> viewFacet;

    @Inject
    protected Provider<SearchFacet> searchFacet;

    @Inject
    protected ExceptionHandler exceptionHandler;

    @Inject
    protected TimingHandler timingHandler;

    @Inject
    protected SecurityHandler securityHandler;

    @Inject
    protected PartialFetchHandler partialFetchHandler;

    @Inject
    protected ConditionalRequestHandler conditionalRequestHandler;

    @Inject
    protected ContentHeadersHandler contentHeadersHandler;

    @Inject
    protected HandlerContributor handlerContributor;

    @Inject
    protected Provider<LastAssetMaintenanceFacet> maintenanceFacet;

    @Inject
    protected Provider<HttpClientFacet> httpClientFacet;

    @Inject
    protected Provider<PurgeUnusedFacet> purgeUnusedFacet;

    @Inject
    protected Provider<NegativeCacheFacet> negativeCacheFacet;

    @Inject
    protected NegativeCacheHandler negativeCacheHandler;

    @Inject
    protected RoutingRuleHandler routingHandler;

    @Inject
    protected LastDownloadedHandler lastDownloadedHandler;

    @Inject
    @Named(CondaFormat.NAME)
    protected CondaPathParser condaPathParser;

    protected CondaRecipeSupport(Type type, Format format) {
        super(type, format);
    }
}

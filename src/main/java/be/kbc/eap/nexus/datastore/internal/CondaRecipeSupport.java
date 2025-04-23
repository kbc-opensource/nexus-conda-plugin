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

    protected Provider<BrowseFacet> browseFacet;

    protected Provider<CondaSecurityFacet> securityFacet;

    protected Provider<ConfigurableViewFacet> viewFacet;

    protected Provider<SearchFacet> searchFacet;

    protected ExceptionHandler exceptionHandler;

    protected TimingHandler timingHandler;

    protected SecurityHandler securityHandler;

    protected PartialFetchHandler partialFetchHandler;

    protected ConditionalRequestHandler conditionalRequestHandler;

    protected ContentHeadersHandler contentHeadersHandler;

    protected HandlerContributor handlerContributor;

    protected Provider<LastAssetMaintenanceFacet> maintenanceFacet;

    protected Provider<HttpClientFacet> httpClientFacet;

    protected Provider<PurgeUnusedFacet> purgeUnusedFacet;

    protected Provider<NegativeCacheFacet> negativeCacheFacet;

    protected NegativeCacheHandler negativeCacheHandler;

    protected RoutingRuleHandler routingRuleHandler;

    protected LastDownloadedHandler lastDownloadedHandler;


    protected CondaPathParser condaPathParser;

    @Inject
    public void setBrowseFacet(Provider<BrowseFacet> browseFacet) {
        this.browseFacet = browseFacet;
    }

    @Inject
    public void setSecurityFacet(Provider<CondaSecurityFacet> securityFacet) {
        this.securityFacet = securityFacet;
    }

    @Inject
    public void setViewFacet(Provider<ConfigurableViewFacet> viewFacet) {
        this.viewFacet = viewFacet;
    }

    @Inject
    public void setSearchFacet(Provider<SearchFacet> searchFacet) {
        this.searchFacet = searchFacet;
    }

    @Inject
    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    @Inject
    public void setTimingHandler(TimingHandler timingHandler) {
        this.timingHandler = timingHandler;
    }

    @Inject
    public void setSecurityHandler(SecurityHandler securityHandler) {
        this.securityHandler = securityHandler;
    }

    @Inject
    public void setPartialFetchHandler(PartialFetchHandler partialFetchHandler) {
        this.partialFetchHandler = partialFetchHandler;
    }

    @Inject
    public void setConditionalRequestHandler(ConditionalRequestHandler conditionalRequestHandler) {
        this.conditionalRequestHandler = conditionalRequestHandler;
    }

    @Inject
    public void setContentHeadersHandler(ContentHeadersHandler contentHeadersHandler) {
        this.contentHeadersHandler = contentHeadersHandler;
    }

    @Inject
    public void setHandlerContributor(HandlerContributor handlerContributor) {
        this.handlerContributor = handlerContributor;
    }

    @Inject
    public void setMaintenanceFacet(Provider<LastAssetMaintenanceFacet> maintenanceFacet) {
        this.maintenanceFacet = maintenanceFacet;
    }

    @Inject
    public void setHttpClientFacet(Provider<HttpClientFacet> httpClientFacet) {
        this.httpClientFacet = httpClientFacet;
    }

    @Inject
    public void setPurgeUnusedFacet(Provider<PurgeUnusedFacet> purgeUnusedFacet) {
        this.purgeUnusedFacet = purgeUnusedFacet;
    }

    @Inject
    public void setNegativeCacheFacet(Provider<NegativeCacheFacet> negativeCacheFacet) {
        this.negativeCacheFacet = negativeCacheFacet;
    }

    @Inject
    public void setNegativeCacheHandler(NegativeCacheHandler negativeCacheHandler) {
        this.negativeCacheHandler = negativeCacheHandler;
    }

    @Inject
    public void setRoutingRuleHandler(RoutingRuleHandler routingRuleHandler) {
        this.routingRuleHandler = routingRuleHandler;
    }

    @Inject
    public void setLastDownloadedHandler(LastDownloadedHandler lastDownloadedHandler) {
        this.lastDownloadedHandler = lastDownloadedHandler;
    }

    @Inject
    public void setCondaPathParser(@Named(CondaFormat.NAME) CondaPathParser condaPathParser) {
        this.condaPathParser = condaPathParser;
    }

    protected CondaRecipeSupport(Type type, Format format) {
        super(type, format);
    }
}

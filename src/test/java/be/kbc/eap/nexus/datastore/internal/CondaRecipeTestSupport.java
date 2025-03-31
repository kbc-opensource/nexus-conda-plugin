package be.kbc.eap.nexus.datastore.internal;

import be.kbc.eap.nexus.CondaPathParser;
import be.kbc.eap.nexus.datastore.CondaSecurityFacet;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
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

import javax.inject.Provider;

public abstract class CondaRecipeTestSupport extends TestSupport {

    @Mock
    protected NegativeCacheHandler negativeCacheHandler;
    @Mock
    protected ExceptionHandler exceptionHandler;
    @Mock
    protected TimingHandler timingHandler;
    @Mock
    protected SecurityHandler securityHandler;
    @Mock
    private PartialFetchHandler partialFetchHandler;
    @Mock
    private ConditionalRequestHandler conditionalRequestHandler;
    @Mock
    private ContentHeadersHandler contentHeadersHandler;
    @Mock
    private HandlerContributor handlerContributor;
    @Mock
    private RoutingRuleHandler routingRuleHandler;
    @Mock
    private LastDownloadedHandler lastDownloadedHandler;

    @Mock
    private CondaPathParser condaPathParser;

    @Mock
    protected CondaSecurityFacet securityFacet;

    private final Provider<CondaSecurityFacet> securityFacetProvider = () -> securityFacet;

    @Mock
    protected BrowseFacet browseFacet;

    private final Provider<BrowseFacet> browseFacetProvider = () -> browseFacet;

    @Mock
    protected HttpClientFacet httpClientFacet;

    private final Provider<HttpClientFacet> httpClientFacetProvider = () -> httpClientFacet;

    @Mock
    protected ConfigurableViewFacet configurableViewFacet;

    private final Provider<ConfigurableViewFacet> configurableViewFacetProvider = () -> configurableViewFacet;

    @Mock
    protected SearchFacet searchFacet;

    private final Provider<SearchFacet> searchFacetProvider = () -> searchFacet;

    @Mock
    protected LastAssetMaintenanceFacet lastAssetMaintenanceFacet;

    private final Provider<LastAssetMaintenanceFacet> lastAssetMaintenanceFacetProvider = () -> lastAssetMaintenanceFacet;

    @Mock
    protected PurgeUnusedFacet purgeUnusedFacet;

    private final Provider<PurgeUnusedFacet> purgeUnusedFacetProvider = () -> purgeUnusedFacet;

    @Mock
    protected NegativeCacheFacet negativeCacheFacet;

    private final Provider<NegativeCacheFacet> negativeCacheFacetProvider = () -> negativeCacheFacet;

    protected <T extends CondaRecipeSupport> void mockHandlers(T underTest) {
        underTest.setNegativeCacheHandler(negativeCacheHandler);
        underTest.setExceptionHandler(exceptionHandler);
        underTest.setTimingHandler(timingHandler);
        underTest.setSecurityHandler(securityHandler);
        underTest.setPartialFetchHandler(partialFetchHandler);
        underTest.setConditionalRequestHandler(conditionalRequestHandler);
        underTest.setContentHeadersHandler(contentHeadersHandler);
        underTest.setHandlerContributor(handlerContributor);
        underTest.setRoutingRuleHandler(routingRuleHandler);
        underTest.setLastDownloadedHandler(lastDownloadedHandler);
        underTest.setCondaPathParser(condaPathParser);
    }

    protected <T extends CondaRecipeSupport> void mockFacets(T underTest) {
        underTest.setBrowseFacet(browseFacetProvider);
        underTest.setHttpClientFacet(httpClientFacetProvider);
        underTest.setSecurityFacet(securityFacetProvider);
        underTest.setViewFacet(configurableViewFacetProvider);
        underTest.setSearchFacet(searchFacetProvider);
        underTest.setMaintenanceFacet(lastAssetMaintenanceFacetProvider);
        underTest.setPurgeUnusedFacet(purgeUnusedFacetProvider);
        underTest.setNegativeCacheFacet(negativeCacheFacetProvider);
    }
}

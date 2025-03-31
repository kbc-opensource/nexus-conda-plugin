package be.kbc.eap.nexus.datastore.internal.proxy;

import be.kbc.eap.nexus.CondaFormat;
import be.kbc.eap.nexus.datastore.CondaContentFacet;
import be.kbc.eap.nexus.datastore.internal.CondaRecipeTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.proxy.ProxyHandler;
import org.sonatype.nexus.repository.types.ProxyType;

import javax.inject.Inject;
import javax.inject.Provider;

import static org.mockito.Mockito.verify;

public class CondaProxyRecipeTest extends CondaRecipeTestSupport {

    @Mock
    private Repository condaProxyRepository;

    @Mock
    private ProxyHandler proxyHandler;

    private CondaProxyRecipe underTest;

    @Mock
    private CondaContentFacet condaContentFacet;

    private Provider<CondaContentFacet> contentFacetProvider = () -> condaContentFacet;

    @Mock
    private CondaProxyFacet condaProxyFacet;

    private Provider<CondaProxyFacet> proxyFacetProvider = () -> condaProxyFacet;

    @Before
    public void setup() {
        underTest = new CondaProxyRecipe(new ProxyType(), new CondaFormat(), contentFacetProvider,
                proxyFacetProvider, proxyHandler);
        mockFacets(underTest);
        mockHandlers(underTest);
    }

    @Test
    public void testExpectedFacetsAreAttached() throws Exception {
        underTest.apply(condaProxyRepository);
        verify(condaProxyRepository).attach(securityFacet);
        verify(condaProxyRepository).attach(httpClientFacet);
        verify(condaProxyRepository).attach(negativeCacheFacet);
        verify(condaProxyRepository).attach(condaProxyFacet);
        verify(condaProxyRepository).attach(condaContentFacet);
        verify(condaProxyRepository).attach(purgeUnusedFacet);
        verify(condaProxyRepository).attach(searchFacet);
        verify(condaProxyRepository).attach(browseFacet);
    }
}

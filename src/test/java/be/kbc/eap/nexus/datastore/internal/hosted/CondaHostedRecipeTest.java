package be.kbc.eap.nexus.datastore.internal.hosted;

import be.kbc.eap.nexus.CondaFormat;
import be.kbc.eap.nexus.datastore.CondaContentFacet;
import be.kbc.eap.nexus.datastore.internal.CondaRecipeTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler;

import javax.inject.Provider;

import static org.mockito.Mockito.verify;

public class CondaHostedRecipeTest extends CondaRecipeTestSupport {

    @Mock
    private Repository condaHostedRepository;
    @Mock
    private CondaHostedHandler condaHostedHandler;
    @Mock
    private LastDownloadedHandler lastDownloadedHandler;

    private CondaHostedRecipe underTest;

    @Mock
    private CondaContentFacet condaContentFacet;

    private final Provider<CondaContentFacet> contentFacetProvider = () -> condaContentFacet;

    @Mock
    private CondaHostedFacetImpl condaHostedFacet;

    private final Provider<CondaHostedFacetImpl> condaHostedFacetProvider = () -> condaHostedFacet;

    @Before
    public void setup() {
        underTest = new CondaHostedRecipe(new HostedType(), new CondaFormat(), contentFacetProvider,
                condaHostedFacetProvider, condaHostedHandler, lastDownloadedHandler);
        mockFacets(underTest);
        mockHandlers(underTest);
    }

    @Test
    public void testExpectedFacetsAreAttached() throws Exception {
        underTest.apply(condaHostedRepository);
        verify(condaHostedRepository).attach(securityFacet);
        verify(condaHostedRepository).attach(httpClientFacet);
        verify(condaHostedRepository).attach(negativeCacheFacet);
        verify(condaHostedRepository).attach(searchFacet);
        verify(condaHostedRepository).attach(browseFacet);
        verify(condaHostedRepository).attach(lastAssetMaintenanceFacet);
        verify(condaHostedRepository).attach(condaContentFacet);
        verify(condaHostedRepository).attach(condaHostedFacet);
    }

}

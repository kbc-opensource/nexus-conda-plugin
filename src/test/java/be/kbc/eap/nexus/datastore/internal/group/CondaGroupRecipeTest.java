package be.kbc.eap.nexus.datastore.internal.group;

import be.kbc.eap.nexus.CondaFormat;
import be.kbc.eap.nexus.datastore.CondaContentFacet;
import be.kbc.eap.nexus.datastore.internal.CondaRecipeTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.types.GroupType;

import javax.inject.Provider;

import static org.mockito.Mockito.verify;

public class CondaGroupRecipeTest extends CondaRecipeTestSupport {

    @Mock
    Repository condaGroupRepository;

    @Mock
    private GroupHandler groupHandler;

    @Mock
    private CondaContentGroupFacet condaGroupFacet;

    private final Provider<CondaContentGroupFacet> condaContentGroupFacetProvider = () -> condaGroupFacet;

    @Mock
    private CondaContentFacet condaContentFacet;

    private final Provider<CondaContentFacet> condaContentFacetProvider = () -> condaContentFacet;

    @Mock
    private CondaGroupMergingHandler groupMergingHandler;

    private CondaGroupRecipe underTest;



    @Before
    public void setup() {
        underTest = new CondaGroupRecipe(new GroupType(), new CondaFormat(), condaContentFacetProvider,
                condaContentGroupFacetProvider, groupHandler, groupMergingHandler);
        mockFacets(underTest);
        mockHandlers(underTest);
    }

    @Test
    public void testExpectedFacetsAreAttached() throws Exception {
        underTest.apply(condaGroupRepository);
        verify(condaGroupRepository).attach(securityFacet);
        verify(condaGroupRepository).attach(condaGroupFacet);
        verify(condaGroupRepository).attach(browseFacet);
        verify(condaGroupRepository).attach(searchFacet);
        verify(condaGroupRepository).attach(purgeUnusedFacet);
        verify(condaGroupRepository).attach(negativeCacheFacet);
        verify(condaGroupRepository).attach(lastAssetMaintenanceFacet);
        verify(condaGroupRepository).attach(condaGroupFacet);
        verify(condaGroupRepository).attach(condaContentFacet);

    }
}

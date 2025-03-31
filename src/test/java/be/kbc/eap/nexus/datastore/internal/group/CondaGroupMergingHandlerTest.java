package be.kbc.eap.nexus.datastore.internal.group;

import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.datastore.CondaContentFacet;
import org.fest.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.view.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class CondaGroupMergingHandlerTest extends TestSupport {

    @Mock
    private Context context;
    @Mock
    private Request request;

    @Mock
    private AttributesMap attributesMap;

    @Mock
    private Status status;

    @Mock
    private CondaPath condaPath;

    @Mock
    private Repository repositoryParent;
    @Mock
    private Repository repository1;
    @Mock
    private Repository repository2;



    @Mock
    private Response response1;
    @Mock
    private Response response2;

    @Mock
    private Content contentParent;

    @Mock
    private Content content1;

    @Mock
    private Content content2;


    @Mock
    private ViewFacet viewFacet1;
    @Mock
    private ViewFacet viewFacet2;

    @Mock
    private CondaContentGroupFacet groupFacet;


    private CondaGroupMergingHandler underTest;


    @Captor
    private ArgumentCaptor<CondaContentFacet> condaContentFacetCaptor;

    @Captor
    private ArgumentCaptor<CondaContentGroupFacet> condaContentGroupFacetCaptor;

    @Captor
    private ArgumentCaptor<Content> contentArgumentCaptor;

    @Captor
    private ArgumentCaptor<Map<Repository, Response>> memberResponsesCaptor;


    @Before
    public void setup() throws Exception {

        when(context.getAttributes()).thenReturn(attributesMap);
        when(context.getRequest()).thenReturn(request);
        when(attributesMap.require(CondaPath.class)).thenReturn(condaPath);
        when(context.getRepository()).thenReturn(repositoryParent);
        when(repositoryParent.getName()).thenReturn("conda-group-repo");
        when(repositoryParent.facet(CondaContentGroupFacet.class)).thenReturn(groupFacet);
        List<Repository> repositories = Lists.newArrayList(repository1, repository2);
        when(groupFacet.members()).thenReturn(repositories);
        when(repository1.getName()).thenReturn("conda-hosted-repo-1");
        when(repository2.getName()).thenReturn("conda-hosted-repo-2");
        when(repository1.facet(ViewFacet.class)).thenReturn(viewFacet1);
        when(repository2.facet(ViewFacet.class)).thenReturn(viewFacet2);
        when(viewFacet1.dispatch(request, context)).thenReturn(response1);
        when(viewFacet2.dispatch(request, context)).thenReturn(response2);
        when(response1.getStatus()).thenReturn(status);
        when(response2.getStatus()).thenReturn(status);
        when(status.getCode()).thenReturn(200);
        when(groupFacet.getRepodataFile(repositoryParent, condaPath)).thenReturn(Optional.of(contentParent));
        //when(groupFacet.getRepodataFile(repository2, condaPath)).thenReturn(Optional.of(content1));
        underTest = new CondaGroupMergingHandler();

    }

    @Test
    public void doGetRepodataJson_isNotStale() throws Exception {

        // Arrange

        when(condaPath.isHash()).thenReturn(false);
        when(condaPath.getPath()).thenReturn("noarch/repodata.json");

        when(groupFacet.isStale(eq(contentParent), any())).thenReturn(false);


        // Act
        Response response = underTest.doGet(context, new GroupHandler.DispatchedRepositories());

        // Assert

        // check return value
        assertEquals(contentParent, response.getPayload());

        // check getrepodatafile called
        verify(groupFacet).getRepodataFile(repositoryParent, condaPath);

        // check isStale is called with content Parent and correct member responses
        verify(groupFacet).isStale(contentArgumentCaptor.capture(), memberResponsesCaptor.capture());
        assertEquals(contentParent, contentArgumentCaptor.getValue());

        List<Map<Repository, Response>> allValues = memberResponsesCaptor.getAllValues();

        assertEquals(1, allValues.size());

        Map<Repository, Response> memberResponses = allValues.get(0);
        assertEquals(2, memberResponses.size());

        assertEquals(response1, memberResponses.get(repository1));
        assertEquals(response2, memberResponses.get(repository2));

        // Check no build of repodata was called

        verify(groupFacet, never()).buildRepodataJson(any(), any(), any());
        verify(groupFacet, never()).buildRepodataZst(any(), any(), any());


    }

    @Test
    public void doGetRepodataJsonZst_isNotStale() throws Exception {

        // Arrange

        when(condaPath.isHash()).thenReturn(false);
        when(condaPath.getPath()).thenReturn("noarch/repodata.json.zst");

        when(groupFacet.isStale(eq(contentParent), any())).thenReturn(false);


        // Act
        Response response = underTest.doGet(context, new GroupHandler.DispatchedRepositories());

        // Assert

        // check return value
        assertEquals(contentParent, response.getPayload());

        // check getrepodatafile called
        verify(groupFacet).getRepodataFile(repositoryParent, condaPath);

        // check isStale is called with content Parent and correct member responses
        verify(groupFacet).isStale(contentArgumentCaptor.capture(), memberResponsesCaptor.capture());
        assertEquals(contentParent, contentArgumentCaptor.getValue());

        List<Map<Repository, Response>> allValues = memberResponsesCaptor.getAllValues();

        assertEquals(1, allValues.size());

        Map<Repository, Response> memberResponses = allValues.get(0);
        assertEquals(2, memberResponses.size());

        assertEquals(response1, memberResponses.get(repository1));
        assertEquals(response2, memberResponses.get(repository2));

        // Check no build of repodata was called

        verify(groupFacet, never()).buildRepodataJson(any(), any(), any());
        verify(groupFacet, never()).buildRepodataZst(any(), any(), any());


    }

    @Test
    public void doGetRepodataJson_isStale() throws Exception {

        // Arrange

        when(condaPath.isHash()).thenReturn(false);
        when(condaPath.getPath()).thenReturn("noarch/repodata.json");

        when(groupFacet.isStale(eq(contentParent), any())).thenReturn(true);

        when(groupFacet.buildRepodataJson(eq(repositoryParent), eq(condaPath), any())).thenReturn(content1);

        // Act
        Response response = underTest.doGet(context, new GroupHandler.DispatchedRepositories());

        // Assert
        assertEquals(response.getPayload(), content1);

        // check getrepodatafile called
        verify(groupFacet).getRepodataFile(repositoryParent, condaPath);

        // check isStale is called with content Parent and correct member responses
        verify(groupFacet).isStale(contentArgumentCaptor.capture(), memberResponsesCaptor.capture());
        assertEquals(contentParent, contentArgumentCaptor.getValue());

        List<Map<Repository, Response>> allValues = memberResponsesCaptor.getAllValues();

        assertEquals(1, allValues.size());

        Map<Repository, Response> memberResponses = allValues.get(0);
        assertEquals(2, memberResponses.size());

        assertEquals(response1, memberResponses.get(repository1));
        assertEquals(response2, memberResponses.get(repository2));

        // Check no build of repodata was called

        verify(groupFacet).buildRepodataJson(eq(repositoryParent), eq(condaPath) , any());
        verify(groupFacet, never()).buildRepodataZst(any(), any(), any());
    }

    @Test
    public void doGetRepodataJsonZst_isStale() throws Exception {

        // Arrange

        when(condaPath.isHash()).thenReturn(false);
        when(condaPath.getPath()).thenReturn("noarch/repodata.json.zst");

        when(groupFacet.isStale(eq(contentParent), any())).thenReturn(true);

        when(groupFacet.buildRepodataZst(eq(repositoryParent), eq(condaPath), any())).thenReturn(content1);

        // Act
        Response response = underTest.doGet(context, new GroupHandler.DispatchedRepositories());

        // Assert
        assertEquals(response.getPayload(), content1);

        // check getrepodatafile called
        verify(groupFacet).getRepodataFile(repositoryParent, condaPath);

        // check isStale is called with content Parent and correct member responses
        verify(groupFacet).isStale(contentArgumentCaptor.capture(), memberResponsesCaptor.capture());
        assertEquals(contentParent, contentArgumentCaptor.getValue());

        List<Map<Repository, Response>> allValues = memberResponsesCaptor.getAllValues();

        assertEquals(1, allValues.size());

        Map<Repository, Response> memberResponses = allValues.get(0);
        assertEquals(2, memberResponses.size());

        assertEquals(response1, memberResponses.get(repository1));
        assertEquals(response2, memberResponses.get(repository2));

        // Check no build of repodata was called

        verify(groupFacet, never()).buildRepodataJson(any(), any(), any());
        verify(groupFacet).buildRepodataZst(eq(repositoryParent), eq(condaPath) , any());
    }
}

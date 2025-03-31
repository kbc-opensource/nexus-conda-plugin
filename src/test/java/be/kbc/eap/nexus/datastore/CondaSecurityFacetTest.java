package be.kbc.eap.nexus.datastore;

import be.kbc.eap.nexus.CondaFormat;
import be.kbc.eap.nexus.util.CondaFormatSecurityContributor;
import org.apache.shiro.authz.AuthorizationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.security.BreadActions;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CondaSecurityFacetTest extends TestSupport {


    @Mock
    Request request;

    @Mock
    Repository repository;

    @Mock
    ContentPermissionChecker contentPermissionChecker;

    @Mock
    VariableResolverAdapter variableResolverAdapter;

    @Mock
    CondaFormatSecurityContributor securityContributor;

    CondaSecurityFacet condaSecurityFacet;


    @Before
    public void setupConfig() throws Exception {
        when(request.getPath()).thenReturn("/noarch/my-artifact-1.0.0.tar.bz2");
        when(request.getAction()).thenReturn(HttpMethods.GET);

        when(repository.getFormat()).thenReturn(new CondaFormat());
        when(repository.getName()).thenReturn("CondaSecurityFacetTest");

        condaSecurityFacet = new CondaSecurityFacet(securityContributor,
                variableResolverAdapter, contentPermissionChecker);

        condaSecurityFacet.attach(repository);
    }

    @Test
    public void testEnsurePermitted() throws Exception {
        when(contentPermissionChecker
                .isPermitted(eq("CondaSecurityFacetTest"), eq(CondaFormat.NAME), eq(BreadActions.READ), any()))
                .thenReturn(true);

        try {
            condaSecurityFacet.ensurePermitted(request);
        }
        catch (AuthorizationException e) {
            fail("expected permitted operation to succeed");
        }
    }

    @Test
    public void testEnsurePermitted_notPermitted() throws Exception {
        try {
            condaSecurityFacet.ensurePermitted(request);
            fail("AuthorizationException should have been thrown");
        }
        catch (AuthorizationException e) {
            //expected
        }

        verify(contentPermissionChecker)
                .isPermitted(eq("CondaSecurityFacetTest"), eq(CondaFormat.NAME), eq(BreadActions.READ), any());
    }
}

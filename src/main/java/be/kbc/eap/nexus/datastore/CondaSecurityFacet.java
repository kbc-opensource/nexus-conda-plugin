package be.kbc.eap.nexus.datastore;

import be.kbc.eap.nexus.util.CondaFormatSecurityContributor;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.SecurityFacetSupport;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class CondaSecurityFacet
        extends SecurityFacetSupport {

    @Inject
    public CondaSecurityFacet(final CondaFormatSecurityContributor securityContributor,
                            @Named("simple") final VariableResolverAdapter variableResolverAdapter,
                            final ContentPermissionChecker contentPermissionChecker) {
        super(securityContributor, variableResolverAdapter, contentPermissionChecker);
    }


}

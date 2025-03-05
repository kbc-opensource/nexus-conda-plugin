package be.kbc.eap.nexus.util;

import be.kbc.eap.nexus.CondaFormat;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.security.RepositoryFormatSecurityContributor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class CondaFormatSecurityContributor
        extends RepositoryFormatSecurityContributor {
    @Inject
    public CondaFormatSecurityContributor(@Named(CondaFormat.NAME) final Format format) {
        super(format);
    }

}

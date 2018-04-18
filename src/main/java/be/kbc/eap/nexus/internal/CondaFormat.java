package be.kbc.eap.nexus.internal;

import org.sonatype.nexus.repository.Format;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(CondaFormat.NAME)
public class CondaFormat extends Format {
    public static final String NAME = "conda";

    public CondaFormat() {
        super(NAME);
    }

}

package be.kbc.eap.nexus;

import org.sonatype.nexus.repository.Format;

import javax.inject.Named;
import javax.inject.Singleton;

@Named(CondaFormat.NAME)
@Singleton
public class CondaFormat extends Format {

    public static final String NAME = "conda";

    public CondaFormat() {
        super(NAME);
    }
}

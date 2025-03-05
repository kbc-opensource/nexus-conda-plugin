package be.kbc.eap.nexus.datastore.internal.browse;


import be.kbc.eap.nexus.CondaFormat;
import org.sonatype.nexus.repository.content.browse.DefaultBrowseNodeGenerator;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(CondaFormat.NAME)
public class CondaBrowseNodeGenerator extends DefaultBrowseNodeGenerator {}
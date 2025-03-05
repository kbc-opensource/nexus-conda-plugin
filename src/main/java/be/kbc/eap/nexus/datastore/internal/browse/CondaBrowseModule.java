package be.kbc.eap.nexus.datastore.internal.browse;

import be.kbc.eap.nexus.CondaFormat;
import org.sonatype.nexus.repository.content.browse.store.FormatBrowseModule;

import javax.inject.Named;

@Named(CondaFormat.NAME)
public class CondaBrowseModule extends FormatBrowseModule<CondaBrowseNodeDAO> {}
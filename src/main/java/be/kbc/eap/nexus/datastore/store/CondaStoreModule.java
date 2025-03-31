package be.kbc.eap.nexus.datastore.store;

import be.kbc.eap.nexus.CondaFormat;
import org.sonatype.nexus.repository.content.store.FormatStoreModule;

import javax.inject.Named;

@Named(CondaFormat.NAME)
public class CondaStoreModule extends FormatStoreModule<CondaContentRepositoryDAO, CondaComponentDAO, CondaAssetDAO, CondaAssetBlobDAO> {}
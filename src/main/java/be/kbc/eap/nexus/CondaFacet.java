package be.kbc.eap.nexus;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

@Facet.Exposed
public interface CondaFacet extends Facet {


    @Nullable
    Content get(CondaPath path) throws IOException;

    Content put(CondaPath path, Payload content) throws IOException;

    boolean delete(CondaPath... paths) throws IOException;

    @Nonnull
    CondaPathParser getCondaPathParser();


    Asset getOrCreateAsset(CondaPath condaPath, Repository repository, String componentName, String componentGroup, String assetName);

}

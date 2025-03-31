package be.kbc.eap.nexus.datastore;

import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.CondaPathParser;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;

@Facet.Exposed
public interface CondaContentFacet extends ContentFacet {
    Optional<FluentAsset> getAsset(String assetPath);

    Content put(CondaPath path, Payload content) throws IOException;

    Content put(CondaPath path, Payload content, String indexJson) throws IOException;

    Content putRepoData(CondaPath path, Payload content);

    boolean delete(CondaPath condaPath);

    @Nonnull
    CondaPathParser getCondaPathParser();

    String rebuildRepoDataJson(Repository repository) throws IOException;
}
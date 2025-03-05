package be.kbc.eap.nexus.datastore;

import be.kbc.eap.nexus.AssetKind;
import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.CondaPathParser;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Facet.Exposed
public interface CondaContentFacet extends ContentFacet {
    Optional<FluentAsset> getAsset(String assetPath);

    Content put(CondaPath path, Payload content) throws IOException;

    Content put(CondaPath path, Payload content, String indexJson) throws IOException;

    boolean delete(CondaPath condaPath);

    boolean delete(List<String> paths);

    @Nonnull
    CondaPathParser getCondaPathParser();

    int deleteComponents(Stream<FluentComponent> components);

    String rebuildRepoDataJson() throws IOException;
}
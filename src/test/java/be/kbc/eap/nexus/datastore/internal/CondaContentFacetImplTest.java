package be.kbc.eap.nexus.datastore.internal;

import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CondaContentFacetImplTest extends TestSupport {
    @Mock
    private FluentAsset asset;

    @Mock
    private Component component;

    @Mock
    private FormatStoreManager formatStoreManager;

    @Captor
    private ArgumentCaptor<Map<String, List<JsonObject>>> architecturesCaptor;

    private CondaContentFacetImpl condaContentFacet;

    @Mock
    private AssetBlob assetBlob;


    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        condaContentFacet = new CondaContentFacetImpl(formatStoreManager, null);
    }

    @Test
    public void testProcessAsset_withValidAsset() {
        String repositoryName = "test-repo";
        Map<String, List<JsonObject>> architectures = new HashMap<>();
        NestedAttributesMap attributes = mock(NestedAttributesMap.class);
        NestedAttributesMap attributes_conda = mock(NestedAttributesMap.class);

        when(asset.path()).thenReturn("some/path/package.tar.bz2");
        when(asset.component()).thenReturn(Optional.of(component));
        when(component.namespace()).thenReturn("noarch");
        when(asset.attributes("attributes")).thenReturn(attributes);
        when(asset.attributes("conda")).thenReturn(attributes_conda);
        when(attributes_conda.contains("build_number")).thenReturn(true);
        when(attributes_conda.get("arch", "noarch")).thenReturn("noarch");
        when(attributes_conda.get("build_number")).thenReturn("1");
        when(attributes_conda.get("buildString")).thenReturn("py_0");
        when(attributes_conda.get("license")).thenReturn("MIT");
        when(attributes_conda.get("license_family")).thenReturn("MIT");
        when(attributes_conda.contains("license_family")).thenReturn(true);
        when(attributes_conda.get("platform", "UNKNOWN")).thenReturn("linux");
        when(attributes_conda.get("subdir")).thenReturn("noarch");
        when(attributes_conda.get("version")).thenReturn("1.0.0");
        when(asset.blob()).thenReturn(Optional.of(assetBlob));
        when(assetBlob.blobSize()).thenReturn(12345L);

        Map<String, String> checksums = new HashMap<>();
        checksums.put("md5", "md5checksum");
        when(assetBlob.checksums()).thenReturn(checksums);
        when(attributes_conda.get("depends")).thenReturn("abc;def");

        condaContentFacet.processAsset(asset, repositoryName, architectures);

        assertTrue(architectures.containsKey("noarch"));
        List<JsonObject> artifacts = architectures.get("noarch");
        assertEquals(1, artifacts.size());
        JsonObject parentArtifact = artifacts.get(0);
        assertTrue(parentArtifact.has("package.tar.bz2"));
        JsonObject artifact = parentArtifact.getAsJsonObject("package.tar.bz2");
        assertEquals("noarch", artifact.get("arch").getAsString());
        assertEquals(1, artifact.get("build_number").getAsInt());
        assertEquals("py_0", artifact.get("build").getAsString());
        assertEquals("MIT", artifact.get("license").getAsString());
        assertEquals("MIT", artifact.get("license_family").getAsString());
        assertEquals("linux", artifact.get("platform").getAsString());
        assertEquals("noarch", artifact.get("subdir").getAsString());
        assertEquals("1.0.0", artifact.get("version").getAsString());
        assertEquals(12345L, artifact.get("size").getAsLong());
        assertEquals("md5checksum", artifact.get("md5").getAsString());
        assertTrue(artifact.get("depends").isJsonArray());
        assertEquals(2, artifact.get("depends").getAsJsonArray().size());
        assertEquals("abc", artifact.get("depends").getAsJsonArray().get(0).getAsString());
        assertEquals("def", artifact.get("depends").getAsJsonArray().get(1).getAsString());
    }

    @Test
    public void testProcessAsset_withInvalidAsset() {
        String repositoryName = "test-repo";
        Map<String, List<JsonObject>> architectures = new HashMap<>();

        when(asset.path()).thenReturn("some/path/repodata.json");

        condaContentFacet.processAsset(asset, repositoryName, architectures);

        assertFalse(architectures.containsKey("noarch"));
    }
}

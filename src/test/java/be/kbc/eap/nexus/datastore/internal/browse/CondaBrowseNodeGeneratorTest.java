package be.kbc.eap.nexus.datastore.internal.browse;

import be.kbc.eap.nexus.CondaFormat;
import be.kbc.eap.nexus.datastore.browse.CondaBrowseNodeGenerator;
import org.junit.Test;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.ComponentData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;

public class CondaBrowseNodeGeneratorTest extends TestSupport {
    private CondaBrowseNodeGenerator underTest = new CondaBrowseNodeGenerator();

    private static final String BASE_VERSION = "0.11.0";

    private static final String SNAPSHOT_VERSION = "1.3.0.dev";

    @Test
    public void should_build_paths_to_base_versioned_asset_which_has_a_component() {
        AssetData asset = new AssetData();
        asset.setPath("/noarch/absl-py/0.11.0/absl-py-0.11.0-pyhd3eb1b0_1.tar.bz2");
        asset.setComponent(aReleaseVersionedComponent());

        List<BrowsePath> browsePaths = underTest.computeAssetPaths(asset);

        assertThat(browsePaths.size(), is(4));
        assertThat(browsePaths, containsInAnyOrder(
                new BrowsePath("noarch", "/noarch/"),
                new BrowsePath("absl-py", "/noarch/absl-py/"),
                new BrowsePath(BASE_VERSION, "/noarch/absl-py/0.11.0/"),
                new BrowsePath("absl-py-0.11.0-pyhd3eb1b0_1.tar.bz2", asset.path())));
    }

    @Test
    public void should_build_paths_for_asset_without_a_component() {
        AssetData asset = new AssetData();
        asset.setPath("/noarch/repodata.json");

        List<BrowsePath> browsePaths = underTest.computeAssetPaths(asset);

        assertThat(browsePaths.size(), is(2));
        assertThat(browsePaths, containsInAnyOrder(
                new BrowsePath("noarch", "/noarch/"),
                new BrowsePath("repodata.json", asset.path())));
    }

    @Test
    public void should_build_paths_to_base_versioned_component() {
        AssetData asset = new AssetData();
        asset.setComponent(aReleaseVersionedComponent());

        List<BrowsePath> browsePaths = underTest.computeComponentPaths(asset);

        assertThat(browsePaths.size(), is(3));
        assertThat(browsePaths, containsInAnyOrder(
                new BrowsePath("noarch", "/noarch/"),
                new BrowsePath("absl-py", "/noarch/absl-py/"),
                new BrowsePath(BASE_VERSION, "/noarch/absl-py/0.11.0/")));
    }

    private Component aReleaseVersionedComponent() {
        ComponentData componentData = createComponent();
        componentData.setVersion(BASE_VERSION);
        formatAttributes(componentData, BASE_VERSION);
        return componentData;
    }

    private void formatAttributes(final ComponentData componentData, final String baseVersion) {
        Map<String, String> formatAttributes = new HashMap<>();
        formatAttributes.put("version", baseVersion);
        componentData.attributes().set(CondaFormat.NAME, formatAttributes);
    }

    private ComponentData createComponent() {
        ComponentData componentData = new ComponentData();
        componentData.setRepositoryId(1);
        componentData.setComponentId(1);
        componentData.setName("absl-py");
        componentData.setNamespace("noarch");
        return componentData;
    }
}

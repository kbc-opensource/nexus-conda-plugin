package be.kbc.eap.nexus.datastore.internal;

import be.kbc.eap.nexus.CondaFormat;
import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.CondaPathParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.store.ComponentStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.sonatype.nexus.repository.content.AttributeOperation.OVERLAY;

public class CondaAttributesHelperTest extends TestSupport {


    @Captor
    private ArgumentCaptor<Map<String, String>> attributesValueCaptor;

    @Mock
    private FluentComponent fluentComponent;

    @Mock
    private FluentAsset fluentAsset;

    @Mock
    private CondaPath condaPath;

    @Mock
    private ComponentStore componentStore;

    @Mock
    private CondaModel condaModel;

    @Mock
    private CondaPathParser condaPathParser;

    private CondaPath.Coordinates coordinates;

    @Before
    public void setup() {
        coordinates = new CondaPath.Coordinates("noarch", "matplotlib", "2.14.5", "py_0", "tar.bz2");
    }

    @Test
    public void shouldPutComponentAttributesInAMap() {
        when(condaPath.getCoordinates()).thenReturn(coordinates);

        mockModel();
        mockComponent();

        CondaAttributesHelper.setCondaAttributes(componentStore, fluentComponent, coordinates, condaModel, 1);
        verify(fluentComponent).attributes(eq(OVERLAY), eq(CondaFormat.NAME), attributesValueCaptor.capture());

        Map<String, String> map = attributesValueCaptor.getValue();
        assertThat(map, hasEntry("arch", condaModel.getArchitecture()));
        assertThat(map, hasEntry("build_number", condaModel.getBuildNumber()));
        assertThat(map, hasEntry("buildString", coordinates.getBuildString()));
        assertThat(map, hasEntry("license", condaModel.getLicense()));
        assertThat(map, hasEntry("license_family", condaModel.getLicenseFamily()));
        assertThat(map, hasEntry("platform", condaModel.getPlatform()));
        assertThat(map, hasEntry("subdir", condaModel.getSubdir()));
        assertThat(map, hasEntry("depends", ""));
        assertThat(map, hasEntry("version", coordinates.getVersion()));
        assertThat(map, hasEntry("packageName", coordinates.getPackageName()));


        assertThat(map.size(), is(10));


    }

    @Test
    public void shouldPutAssetAttributesInAMap() {
        when(condaPath.getCoordinates()).thenReturn(coordinates);

        mockModel();

        CondaAttributesHelper.setCondaAttributes(fluentAsset, condaPath, Optional.of(condaModel));

        verify(fluentAsset).attributes(eq(OVERLAY), eq(CondaFormat.NAME), attributesValueCaptor.capture());
        Map<String, String> map = attributesValueCaptor.getValue();

        assertThat(map, hasEntry("arch", condaModel.getArchitecture()));
        assertThat(map, hasEntry("build_number", condaModel.getBuildNumber()));
        assertThat(map, hasEntry("buildString", coordinates.getBuildString()));
        assertThat(map, hasEntry("license", condaModel.getLicense()));
        assertThat(map, hasEntry("license_family", condaModel.getLicenseFamily()));
        assertThat(map, hasEntry("platform", condaModel.getPlatform()));
        assertThat(map, hasEntry("subdir", condaModel.getSubdir()));
        assertThat(map, hasEntry("depends", ""));
        assertThat(map, hasEntry("version", coordinates.getVersion()));
        assertThat(map, hasEntry("packageName", coordinates.getPackageName()));


        assertThat(map.size(), is(10));

    }

    @Test
    public void testAssetKind_withCoordinates() {
        CondaPath.Coordinates coordinates = new CondaPath.Coordinates("namespace", "packageName", "version", "buildString", "extension");
        when(condaPath.getCoordinates()).thenReturn(coordinates);

        String kind = CondaAttributesHelper.assetKind(condaPath, condaPathParser);
        assertEquals("ARTIFACT", kind);
    }

    @Test
    public void testAssetKind_withRepodata() {
        when(condaPath.getCoordinates()).thenReturn(null);
        when(condaPathParser.isRepodata(condaPath)).thenReturn(true);

        String kind = CondaAttributesHelper.assetKind(condaPath, condaPathParser);
        assertEquals("REPODATA", kind);
    }

    @Test
    public void testAssetKind_withRepodataZst() {
        when(condaPath.getCoordinates()).thenReturn(null);
        when(condaPathParser.isRepodataZst(condaPath)).thenReturn(true);

        String kind = CondaAttributesHelper.assetKind(condaPath, condaPathParser);
        assertEquals("REPODATA", kind);
    }

    @Test
    public void testAssetKind_withOther() {
        when(condaPath.getCoordinates()).thenReturn(null);
        when(condaPathParser.isRepodata(condaPath)).thenReturn(false);
        when(condaPathParser.isRepodataZst(condaPath)).thenReturn(false);

        String kind = CondaAttributesHelper.assetKind(condaPath, condaPathParser);
        assertEquals("OTHER", kind);
    }

    private void mockModel() {
        when(condaModel.getArchitecture()).thenReturn("noarch");
        when(condaModel.getPlatform()).thenReturn("UNKNOWN");
        when(condaModel.getLicense()).thenReturn("license");
        when(condaModel.getLicenseFamily()).thenReturn("");
        when(condaModel.getBuildNumber()).thenReturn("py_0");
        when(condaModel.getDepends()).thenReturn(new ArrayList<>());
        when(condaModel.getSubdir()).thenReturn("noarch");

    }


    private void mockComponent() {
        when(fluentComponent.namespace()).thenReturn(coordinates.getNamespace());
        when(fluentComponent.name()).thenReturn(coordinates.getPackageName());
        when(fluentComponent.version()).thenReturn(coordinates.getVersion());
    }
}

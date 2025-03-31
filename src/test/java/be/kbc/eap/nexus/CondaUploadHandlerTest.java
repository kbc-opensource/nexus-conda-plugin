package be.kbc.eap.nexus;

import be.kbc.eap.nexus.datastore.CondaContentFacet;
import be.kbc.eap.nexus.util.CondaPathParserImpl;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.FluentBlobs;
import org.sonatype.nexus.repository.content.security.internal.SimpleVariableResolverAdapter;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.upload.*;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.selector.VariableSource;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type.STRING;

public class CondaUploadHandlerTest extends TestSupport {

    private static final String REPO_NAME = "conda-hosted";

    private CondaUploadHandler underTest;

    @Mock
    Repository repository;


    @Mock
    CondaContentFacet condaFacet;

    @Mock
    PartPayload condaPayload;

    @Mock
    PartPayload indexPayload;

    @Mock
    TempBlob tempBlob;

    @Mock
    private ContentPermissionChecker contentPermissionChecker;

    @Captor
    private ArgumentCaptor<VariableSource> captor;

    @Before
    public void setup() throws IOException {
        when(contentPermissionChecker.isPermitted(eq(REPO_NAME), eq(CondaFormat.NAME), eq(BreadActions.EDIT), any()))
                .thenReturn(true);

        underTest = new CondaUploadHandler(new CondaPathParserImpl(), emptySet(), new SimpleVariableResolverAdapter(), contentPermissionChecker);

        when(repository.getName()).thenReturn(REPO_NAME);
        when(repository.getFormat()).thenReturn(new CondaFormat());
        when(repository.facet(CondaContentFacet.class)).thenReturn(condaFacet);
        //when(repository.facet(MavenMetadataRebuildFacet.class)).thenReturn(mavenMetadataRebuildFacet);
        FluentBlobs blobs = mock(FluentBlobs.class);
        when(condaFacet.blobs()).thenReturn(blobs);
        when(blobs.ingest(any(Payload.class), any())).thenReturn(tempBlob);
//        when(condaFacet.getVersionPolicy()).thenReturn(VersionPolicy.RELEASE);
//        when(condaFacet.layoutPolicy()).thenReturn(LayoutPolicy.STRICT);
        Content content = mock(Content.class);
        AttributesMap attributesMap = mock(AttributesMap.class);
        Asset assetPayload = mock(Asset.class);
        when(attributesMap.get(Asset.class)).thenReturn(assetPayload);
        when(attributesMap.require(eq(Content.CONTENT_LAST_MODIFIED), eq(DateTime.class))).thenReturn(DateTime.now());
        AssetBlob blob = mock(AssetBlob.class);
        when(assetPayload.blob()).thenReturn(Optional.of(blob));
        Map<String, String> checksums = Collections.singletonMap(
                HashAlgorithm.SHA1.name(),
                "da39a3ee5e6b4b0d3255bfef95601890afd80709");
        when(blob.checksums()).thenReturn(checksums);
        when(content.getAttributes()).thenReturn(attributesMap);
        when(condaFacet.put(any(), any())).thenReturn(content);
        when(condaFacet.put(any(), any(), any())).thenReturn(content);
    }

    @Test
    public void testGetDefinition() {
        UploadDefinition def = underTest.getDefinition();
        assertThat(def.isMultipleUpload(), is(false));
        assertThat(def.getComponentFields().size(), is(0));
        assertThat(def.getAssetFields(), contains(
            field("path", "Path",null, false, STRING, null),
            field("index", "Index",null, false, STRING, null)
        ));

    }


    @Test
    public void testHandle() throws IOException {
        ComponentUpload componentUpload = new ComponentUpload();

        AssetUpload assetUpload = new AssetUpload();
        assetUpload.getFields().put("path", "/noarch/absl-py-0.11.0-pyhd3eb1b0_1.tar.bz2");
        assetUpload.getFields().put("index", "{}");
        assetUpload.setPayload(condaPayload);

        componentUpload.getAssetUploads().add(assetUpload);

        UploadResponse uploadResponse = underTest.handle(repository, componentUpload);

        ArgumentCaptor<CondaPath> pathCapture = ArgumentCaptor.forClass(CondaPath.class);
        verify(condaFacet, times(1)).put(pathCapture.capture(), any(Payload.class), any(String.class));

        List<CondaPath> paths = pathCapture.getAllValues();

        assertThat(paths, hasSize(1));

        CondaPath path = paths.get(0);
        assertNotNull(path);
        assertThat(path.getPath(), is("noarch/absl-py-0.11.0-pyhd3eb1b0_1.tar.bz2"));
        assertCoordinates(path.getCoordinates(), "noarch", "absl-py", "0.11.0", "pyhd3eb1b0_1", "tar.bz2");

        verify(contentPermissionChecker, times(1)).isPermitted(eq(REPO_NAME), eq(CondaFormat.NAME), eq(BreadActions.EDIT),
                captor.capture());

        List<VariableSource> sources = captor.getAllValues();

        assertVariableSource(sources.get(0), "/noarch/absl-py-0.11.0-pyhd3eb1b0_1.tar.bz2", "noarch",
                "absl-py", "0.11.0", "pyhd3eb1b0_1", "tar.bz2");

        verify(condaFacet, times(1)).rebuildRepoDataJson(repository);
    }

    @Test
    public void testHandle_unauthorized() throws IOException {
        when(contentPermissionChecker.isPermitted(eq(REPO_NAME), eq(CondaFormat.NAME), eq(BreadActions.EDIT), any()))
                .thenReturn(false);

        ComponentUpload componentUpload = new ComponentUpload();


        AssetUpload assetUpload = new AssetUpload();
        assetUpload.getFields().put("path", "/noarch/absl-py-0.11.0-pyhd3eb1b0_1.tar.bz2");
        assetUpload.getFields().put("index", "{}");
        assetUpload.setPayload(condaPayload);
        componentUpload.getAssetUploads().add(assetUpload);

        try {
            underTest.handle(repository, componentUpload);
            fail("Expected validation exception");
        }
        catch (ValidationErrorsException e) {
            assertThat(e.getValidationErrors().size(), is(1));
            assertThat(e.getValidationErrors().get(0).getMessage(),
                    is("Not authorized for requested path '/noarch/absl-py-0.11.0-pyhd3eb1b0_1.tar.bz2'"));
        }
    }

    private static void assertVariableSource(final VariableSource source,
                                             final String path,
                                             final String namespace,
                                             final String packageName,
                                             final String version,
                                             final String buildVersion,
                                             final String extension)
    {
        assertThat(source.getVariableSet(), hasSize(7));
        assertThat(source.get("format"), is(Optional.of(CondaFormat.NAME)));
        assertThat(source.get("path"), is(Optional.of(path)));
        assertThat(source.get("coordinate.namespace"), is(Optional.of(namespace)));
        assertThat(source.get("coordinate.packageName"), is(Optional.of(packageName)));
        assertThat(source.get("coordinate.version"), is(Optional.of(version)));
        assertThat(source.get("coordinate.buildVersion"), is(Optional.of(buildVersion)));
        assertThat(source.get("coordinate.extension"), is(Optional.of(extension)));
    }

    private static void assertCoordinates(final CondaPath.Coordinates actual,
                                          final String namespace,
                                          final String packageName,
                                          final String version,
                                          final String buildString,
                                          final String extension)
    {
        assertThat(actual.getNamespace(), is(namespace));
        assertThat(actual.getPackageName(), is(packageName));
        assertThat(actual.getVersion(), is(version));
        assertThat(actual.getBuildString(), is(buildString));
        assertThat(actual.getExtension(), is(extension));


    }

    private UploadFieldDefinition field(final String name,
                                        final String displayName,
                                        final String helpText,
                                        final boolean optional,
                                        final UploadFieldDefinition.Type type,
                                        final String group)
    {
        return new UploadFieldDefinition(name, displayName, helpText, optional, type, group);
    }
}

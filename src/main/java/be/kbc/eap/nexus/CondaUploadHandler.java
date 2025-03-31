package be.kbc.eap.nexus;

import be.kbc.eap.nexus.datastore.CondaContentFacet;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.upload.*;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.prependIfMissing;


@Named(CondaFormat.NAME)
@Singleton
public class CondaUploadHandler
    extends UploadHandlerSupport  {


    private static final String PATH = "path";
    private static final String INDEX = "index";

    private UploadDefinition definition;
    private final CondaPathParser parser;
    private final VariableResolverAdapter variableResolverAdapter;
    private final ContentPermissionChecker contentPermissionChecker;

    @Inject
    public CondaUploadHandler(final CondaPathParser parser,
                              final Set<UploadDefinitionExtension> uploadDefinitionExtensions, VariableResolverAdapter variableResolverAdapter, ContentPermissionChecker contentPermissionChecker) {
        super(uploadDefinitionExtensions);
        this.parser = parser;
        this.variableResolverAdapter = variableResolverAdapter;
        this.contentPermissionChecker = contentPermissionChecker;
    }

    @Override
    public UploadResponse handle(Repository repository, ComponentUpload componentUpload) throws IOException {
        return doUpload(repository, componentUpload);
    }

    private UploadResponse doUpload(final Repository repository, final ComponentUpload componentUpload) throws IOException {
        CondaContentFacet facet = repository.facet(CondaContentFacet.class);
        ContentAndAssetPathResponseData responseData;

        String basePath = "/";

        CondaPath condaPath = getCondaPath(basePath, componentUpload.getAssetUploads().get(0));
        String path = prependIfMissing(condaPath.getPath(), "/");

        ensurePermitted(repository.getName(), CondaFormat.NAME, path, toMap(condaPath.getCoordinates()));

        try {
            // store uploaded artifact
            responseData = createAssets(repository, basePath, componentUpload.getAssetUploads());
            // update metadata
            facet.rebuildRepoDataJson(repository);
        }
        finally {

        }

        return new UploadResponse(responseData.getContent(), responseData.getAssetPaths());
    }

    protected Map<String, String> toMap(final CondaPath.Coordinates coordinates) {
        Map<String, String> map = new HashMap<>();
        if (coordinates != null) {
            map.put("namespace", coordinates.getNamespace());
            map.put("packageName", coordinates.getPackageName());
            map.put("version", coordinates.getVersion());
            map.put("buildVersion", coordinates.getBuildString());
            map.put("extension", coordinates.getExtension());
        }
        return map;
    }

    private ContentAndAssetPathResponseData createAssets(final Repository repository,
                              final String basePath,
                              final List<AssetUpload> assetUploads) throws IOException {

        ContentAndAssetPathResponseData responseData = new ContentAndAssetPathResponseData();

        for(AssetUpload asset: assetUploads) {

            log.debug("Processing asset of size " + asset.getPayload().getSize());

            CondaPath condaPath = getCondaPath(basePath, asset);
            String indexJson = asset.getFields().get(INDEX);

            Content content = storeAssetContent(repository, condaPath, asset.getPayload(), indexJson);
            if(responseData.getContent() == null) {
                responseData.setContent(content);
            }
            responseData.addAssetPath(condaPath.getPath());
            if (responseData.getCoordinates() == null) {
                responseData.setCoordinates(condaPath.getCoordinates());
            }
        }

        return responseData;
    }

    private CondaPath getCondaPath(final String basePath, final AssetUpload asset) {
        StringBuilder path = new StringBuilder(basePath);
        String assetPath = asset.getFields().get(PATH);
        if(!Strings2.isEmpty(assetPath)) {
            if(assetPath.startsWith("/")) {
                assetPath = assetPath.substring(1);
            }
            path.append(assetPath);
        }
        return parser.parsePath(path.toString());
    }

    protected Content storeAssetContent(final Repository repository,
                                        final CondaPath condaPath,
                                        final Payload payload,
                                        final String indexJson) throws IOException
    {
        CondaContentFacet condaFacet = repository.facet(CondaContentFacet.class);
        Content content = condaFacet.put(condaPath, payload, indexJson);
        //putChecksumFiles(condaFacet, condaPath, content);

        return content;
    }

//    private void putChecksumFiles(final CondaContentFacet facet, final CondaPath path, final Content content) throws IOException {
//        DateTime dateTime = content.getAttributes().require(Content.CONTENT_LAST_MODIFIED, DateTime.class);
//        Map<HashAlgorithm, HashCode> hashes = CondaFacetUtils.getHashAlgorithmFromContent(content.getAttributes());
//        CondaFacetUtils.addHashes(facet, path, hashes, dateTime);
//    }

    @Override
    public UploadDefinition getDefinition() {
        if(definition == null) {
            List<UploadFieldDefinition> componentFields = Arrays.asList();

            List<UploadFieldDefinition> assetFields = Arrays.asList(
                    new UploadFieldDefinition(PATH, false, UploadFieldDefinition.Type.STRING),
                    new UploadFieldDefinition(INDEX, false, UploadFieldDefinition.Type.STRING));


            definition = getDefinition(CondaFormat.NAME, false, componentFields, assetFields, null);
        }
        return definition;
    }

    @Override
    public VariableResolverAdapter getVariableResolverAdapter() {
        return variableResolverAdapter;
    }

    @Override
    public ContentPermissionChecker contentPermissionChecker() {
        return contentPermissionChecker;
    }

    private static class ContentAndAssetPathResponseData {
        Content content;
        List<String> assetPaths = new ArrayList();
        CondaPath.Coordinates coordinates;

        public void setContent(final Content content) {
            this.content = content;
        }

        public void setCoordinates(final CondaPath.Coordinates coordinates) {
            this.coordinates = coordinates;
        }

        public void addAssetPath(final String assetPath) {
            this.assetPaths.add(assetPath);
        }

        public Content getContent() {return this.content;}

        public CondaPath.Coordinates getCoordinates() {
            return this.coordinates;
        }

        public List<String> getAssetPaths() { return this.assetPaths;}

        public UploadResponse uploadResponse() {
            return new UploadResponse(content, assetPaths);
        }
    }
}

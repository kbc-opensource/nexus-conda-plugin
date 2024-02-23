package be.kbc.eap.nexus.internal;

import be.kbc.eap.nexus.CondaFacet;
import be.kbc.eap.nexus.CondaCoordinatesHelper;
import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.CondaPathParser;
import com.github.luben.zstd.Zstd;
import com.google.gson.*;
import org.sonatype.nexus.blobstore.api.*;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.storage.*;
import org.sonatype.nexus.repository.transaction.*;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

@Named
public class CondaFacetImpl extends FacetSupport implements CondaFacet {
    private final Map<String, CondaPathParser> condaPathParsers;
    private CondaPathParser condaPathParser;
    private final BlobStoreManager blobStoreManager;

    @Inject
    public CondaFacetImpl(final Map<String, CondaPathParser> condaPathParserMap, BlobStoreManager blobStoreManager) {
        this.condaPathParsers = checkNotNull(condaPathParserMap);
        this.blobStoreManager = blobStoreManager;
    }

    private static final List<HashAlgorithm> hashAlgorithms = Arrays.asList(MD5, SHA1);

    // TODO: raw does not have config, this method is here only to have this bundle do Import-Package org.sonatype.nexus.repository.config
    // TODO: as FacetSupport subclass depends on it. Actually, this facet does not need any kind of configuration
    // TODO: it's here only to circumvent this OSGi/maven-bundle-plugin issue.
    @Override
    protected void doValidate(final Configuration configuration) throws Exception {
        // empty
    }

    @Override
    protected void doInit(Configuration configuration) throws Exception {
        super.doInit(configuration);

        condaPathParser = checkNotNull(condaPathParsers.get(getRepository().getFormat().getValue()));
        getRepository().facet(StorageFacet.class).registerWritePolicySelector(new CondaWritePolicySelector());
    }

    @Nullable
    @Override
    @TransactionalTouchBlob
    public Content get(final CondaPath path) {
        StorageTx tx = UnitOfWork.currentTx();

        final Asset asset = findAsset(tx, path);
        if (asset == null) {
            return null;
        }
        if (asset.markAsDownloaded()) {
            tx.saveAsset(asset);
        }

        final Blob blob = tx.requireBlob(asset.requireBlobRef());
        return toContent(asset, blob);
    }

    @Override
    @TransactionalStoreBlob
    public Content put(final CondaPath path, final Payload content) throws IOException {
        return put(path, content, null);
    }

    @Override
    @TransactionalStoreBlob
    public Content put(final CondaPath path, final Payload content, String indexJson) throws IOException {
        log.info("CondaFacetImpl - put - " + path.getPath() + " - " + content.getSize());
        StorageFacet storageFacet = facet(StorageFacet.class);
        try (TempBlob tempBlob = storageFacet.createTempBlob(content, hashAlgorithms)) {

            return doPutContent(path, tempBlob, content, indexJson);
        }

    }
    @Override
    @TransactionalDeleteBlob
    public boolean delete(CondaPath... paths) throws IOException {
        final StorageTx tx = UnitOfWork.currentTx();

        boolean result = false;
        for (CondaPath path : paths) {
            log.trace("DELETE {} : {}", getRepository().getName(), path.getPath());
            if (path.getCoordinates() != null) {
                result = deleteArtifact(path, tx) || result;
            } else {
                result = deleteFile(path, tx) || result;
            }
        }
        return result;
    }

    private boolean deleteArtifact(final CondaPath path, final StorageTx tx) {
        final Component component = findComponent(tx, tx.findBucket(getRepository()), path);
        if (component == null) {
            return false;
        }
        final Asset asset = findAsset(tx, path);
        if (asset == null) {
            return false;
        }
        tx.deleteAsset(asset);
        if (!tx.browseAssets(component).iterator().hasNext()) {
            tx.deleteComponent(component);
        }
        return true;
    }

    private boolean deleteFile(final CondaPath path, final StorageTx tx) {
        final Asset asset = findAsset(tx, path);
        if (asset == null) {
            return false;
        }
        tx.deleteAsset(asset);
        return true;
    }

    @Nonnull
    @Override
    public CondaPathParser getCondaPathParser() {
        return condaPathParser;
    }

    private String getJsonAttribute(JsonObject json, String attribute, String defaultValue) {
        if(json.has(attribute) && !json.get(attribute).isJsonNull()) {
            return json.get(attribute).getAsString();
        }
        return defaultValue;
    }

    private void fillAttributesFromJson(String indexJson, NestedAttributesMap attributesMap) {
        Gson gson = new Gson();
        JsonObject indexRoot = gson.fromJson(indexJson, JsonObject.class);
        attributesMap.set("arch", getJsonAttribute(indexRoot, "arch", "noarch"));
        attributesMap.set("build_number", getJsonAttribute(indexRoot, "build_number", ""));
        attributesMap.set("license", getJsonAttribute(indexRoot, "license", ""));
        attributesMap.set("license_family", getJsonAttribute(indexRoot, "license_family", ""));
        attributesMap.set("platform", getJsonAttribute(indexRoot, "platform", "UNKNOWN"));
        attributesMap.set("subdir", getJsonAttribute(indexRoot, "subdir", ""));

        JsonArray depends = indexRoot.get("depends").getAsJsonArray();
        List<String> sDepends = new ArrayList<>();

        for(JsonElement depend : depends) {
            sDepends.add(depend.getAsString());
        }

        attributesMap.set("depends", String.join(";", sDepends));
    }

    protected Content doPutContent(final CondaPath path, final TempBlob tempBlob, final Payload payload, final String indexJson)
            throws IOException {
        StorageTx tx = UnitOfWork.currentTx();

        Asset asset = null;
        if (path.getCoordinates() == null) {
            asset = getOrCreateAsset(path, getRepository(), path.getFileName(), CondaCoordinatesHelper.getGroup(path.getPath()), path.getFileName());
        } else {
            asset = getOrCreateAsset(path, getRepository(), path.getCoordinates().getPackageName(), CondaCoordinatesHelper.getGroup(path.getPath()), path.getFileName());
        }

        AttributesMap contentAttributes = null;
        if (payload instanceof Content) {
            contentAttributes = ((Content) payload).getAttributes();
        }

        Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));

        if(indexJson != null) {
            fillAttributesFromJson(indexJson, asset.formatAttributes());
        }

        AssetBlob assetBlob = tx.setBlob(
                asset,
                path.getPath(),
                tempBlob,
                null,
                payload.getContentType(),
                false
        );

        tx.saveAsset(asset);

        return toContent(asset, assetBlob.getBlob());
    }

    @TransactionalStoreMetadata
    public Asset getOrCreateAsset(final CondaPath condaPath, final Repository repository, final String componentName, final String componentGroup,
                                  final String assetName) {
        final StorageTx tx = UnitOfWork.currentTx();

        final Bucket bucket = tx.findBucket(getRepository());
        log.debug("Find component with name " + componentName + " and path: " + condaPath.getPath());

        Component component = CondaFacetUtils.findComponent(tx, repository, condaPath);
        Asset asset;
        if (component == null) {
            // CREATE
            log.debug("Create new component and asset");
            if(condaPath.getCoordinates()!=null) {
                component = tx.createComponent(bucket, getRepository().getFormat())
                        .group(componentGroup)
                        .name(componentName)
                        .version(condaPath.getCoordinates().getVersion());
            }
            else {
                component = tx.createComponent(bucket, getRepository().getFormat())
                        .group(componentGroup)
                        .name(componentName);
            }

            if(condaPath.getCoordinates()!=null) {
                NestedAttributesMap componentAttributes = component.formatAttributes();
                componentAttributes.set("packageName", condaPath.getCoordinates().getPackageName());
                componentAttributes.set("version", condaPath.getCoordinates().getVersion());
                componentAttributes.set("buildString", condaPath.getCoordinates().getBuildString());
            }

            tx.saveComponent(component);

            asset = tx.createAsset(bucket, component);
            if(condaPath.getCoordinates()!=null) {
                NestedAttributesMap assetAttributes = asset.formatAttributes();
                assetAttributes.set("packageName", condaPath.getCoordinates().getPackageName());
                assetAttributes.set("version", condaPath.getCoordinates().getVersion());
                assetAttributes.set("buildString", condaPath.getCoordinates().getBuildString());
            }

            asset.name(condaPath.getPath());
        } else {
            // UPDATE
            log.debug("Component exists: " + component.group() + " - " + component.name());
            log.debug("Find asset " + condaPath.getPath());
            asset = tx.findAssetWithProperty(P_NAME, condaPath.getPath(), component);
            if(asset == null) {
                log.info("Asset doesn't exist.  Create it");
                asset = tx.createAsset(bucket, component);
                if(condaPath.getCoordinates()!=null) {
                    NestedAttributesMap assetAttributes = asset.formatAttributes();
                    assetAttributes.set("packageName", condaPath.getCoordinates().getPackageName());
                    assetAttributes.set("version", condaPath.getCoordinates().getVersion());
                    assetAttributes.set("buildString", condaPath.getCoordinates().getBuildString());
                }

                asset.name(condaPath.getPath());
            }
            else {
                log.debug("Asset exists " + asset.name() + " - " + asset.size());
            }
        }

        asset.markAsDownloaded();

        return asset;
    }

//    @Override
//    @TransactionalDeleteBlob
//    public boolean delete(final String path) throws IOException {
//        StorageTx tx = UnitOfWork.currentTx();
//
//        final Component component = findComponent(tx, tx.findBucket(getRepository()), path);
//        if (component == null) {
//            return false;
//        }
//
//        tx.deleteComponent(component);
//        return true;
//    }

//    @Override
//    @TransactionalTouchMetadata
//    public void setCacheInfo(final CondaPath path, final Content content, final CacheInfo cacheInfo) throws IOException {
//        StorageTx tx = UnitOfWork.currentTx();
//        Bucket bucket = tx.findBucket(getRepository());
//
//        // by EntityId
//        Asset asset = Content.findAsset(tx, bucket, content);
//        if (asset == null) {
//            // by format coordinates
//            Component component = tx.findComponentWithProperty(P_NAME, path.getPath(), bucket);
//            if (component != null) {
//                asset = tx.firstAsset(component);
//            }
//        }
//        if (asset == null) {
//            log.debug("Attempting to set cache info for non-existent raw component {}", path);
//            return;
//        }
//
//        log.debug("Updating cacheInfo of {} to {}", path, cacheInfo);
//        CacheInfo.applyToAsset(asset, cacheInfo);
//        tx.saveAsset(asset);
//    }

    private Component findComponent(StorageTx tx, Bucket bucket, CondaPath path) {
        return tx.findComponentWithProperty(P_NAME, path.getPath(), bucket);
    }

    private Asset findAsset(StorageTx tx, CondaPath path) {
        Repository repository = getRepository();
        log.info("find Asset " + path.getPath() + " in repository " + repository.getName());

        return tx.findAssetWithProperty(P_NAME, path.getPath(), tx.findBucket(getRepository()));
    }

    private Content toContent(final Asset asset, final Blob blob) {
        final Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
        Content.extractFromAsset(asset, hashAlgorithms, content.getAttributes());
        log.debug("Convert asset to content - Content Size: " + content.getSize() + " - Asset size: " + asset.size().toString());
        return content;
    }

    private void compressAndSaveRepoDataJsonZst(String sourcePath, String targetPath) throws IOException {
        StorageTx tx = UnitOfWork.currentTx();
        Bucket bucket = tx.findBucket(getRepository());
        Asset sourceAsset = tx.findAssetWithProperty(P_NAME, sourcePath, bucket);

        if (sourceAsset == null) {
            log.info("Source asset for Zstd compression not found: " + sourcePath);
            return;
        }

        BlobRef blobRef = sourceAsset.requireBlobRef();
        BlobStore blobStore = blobStoreManager.get(blobRef.getStore());

        if (blobStore == null) {
            log.info("BlobStore not found for blobRef: " + blobRef);
            return;
        }

        BlobId blobId = blobRef.getBlobId();
        Blob blob = blobStore.get(blobId);

        if (blob == null) {
            log.info("Blob not found for asset: " + sourcePath);
            return;
        }

        try (InputStream sourceInputStream = blob.getInputStream()) {
            byte[] compressedBytes = compressInputStream(sourceInputStream);

            Payload payload = new BytesPayload(compressedBytes, "application/zstd");
            put(new CondaPath(targetPath, null), payload);
        } catch (IOException e) {
            log.info("Error compressing repodata.json to ZST format", e);
            throw e;
        }
    }

    private byte[] compressInputStream(InputStream inputStream) throws IOException {

        final int bufLen = 1024;
        byte[] buf = new byte[bufLen];
        int readLen;

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
                outputStream.write(buf, 0, readLen);

            return ZstdUtils.compressZstdData(outputStream.toByteArray());
        } catch (IOException e) {
            throw e;
        }

    }

    @Override
    @Transactional
    public void rebuildRepoDataJson() throws IOException {
        StorageTx tx = UnitOfWork.currentTx();

        Map<String, List<JsonObject>> architectures = new HashMap<>();
        architectures.put("noarch", new ArrayList<>());

        // fill our list with all the components/assets and store the metadata to build our repodata.json
        final Bucket bucket = tx.findBucket(getRepository());
        for(Asset asset : tx.browseAssets(bucket)) {
            try {
                if (!asset.name().endsWith("repodata.json") || !asset.name().endsWith("repodata.json.zst")) {
                    log.debug("Processing " + asset.name());
                    Component component = tx.findComponent(asset.componentId());
                    if (component == null)
                        continue;
                    ;
                    if (!architectures.containsKey(component.group())) {
                        architectures.put(component.group(), new ArrayList<>());
                    }
                    List<JsonObject> artifacts = architectures.get(component.group());
                    NestedAttributesMap formatAttributes = asset.formatAttributes();

                    if (!formatAttributes.contains("build_number")) {
                        continue;
                    }
                    JsonObject artifact = new JsonObject();
                    artifact.addProperty("arch", formatAttributes.get("arch", "noarch").toString());
                    artifact.addProperty("build_number", Long.parseLong(formatAttributes.get("build_number").toString()));
                    artifact.addProperty("build", formatAttributes.get("buildString").toString());
                    artifact.addProperty("license", formatAttributes.get("license").toString());
                    if (formatAttributes.contains("license_family")) {
                        artifact.addProperty("license_family", formatAttributes.get("license_family").toString());
                    }
                    artifact.addProperty("name", component.name());
                    artifact.addProperty("platform", formatAttributes.get("platform", "UNKNOWN").toString());
                    artifact.addProperty("subdir", formatAttributes.get("subdir").toString());
                    artifact.addProperty("version", formatAttributes.get("version").toString());
                    artifact.addProperty("size", asset.size());
                    artifact.addProperty("md5", asset.getChecksum(HashAlgorithm.MD5).toString());
                    String depends = formatAttributes.get("depends").toString();

                    JsonArray jDepends = new JsonArray();
                    if (!Strings2.isEmpty(depends)) {
                        String[] parts = depends.split(";");
                        for (String part : parts) {
                            jDepends.add(new JsonPrimitive(part));
                        }
                    }
                    artifact.add("depends", jDepends);
                    CondaPath condaPath = new CondaPath(asset.name(), null);
                    JsonObject parentArtifact = new JsonObject();
                    parentArtifact.add(condaPath.getFileName(), artifact);
                    artifacts.add(parentArtifact);
                }
            }
            catch(Exception ex) {
                log.error("[rebuildRepoDataJson] Error processing asset " + asset.name(), ex);
            }
        }

        // now build the repodata.json files

        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        for(String arch : architectures.keySet()) {

            log.debug("Building repodata.json for " + arch);

            JsonObject root = new JsonObject();
            JsonObject info = new JsonObject();
            info.addProperty("subdir", arch);
            root.add("info", info);
            JsonObject packages = new JsonObject();

            for(JsonObject artifact : architectures.get(arch)) {
                Set<Map.Entry<String, JsonElement>> entries = artifact.entrySet();
                for (Map.Entry<String, JsonElement> entry : entries) {
                    log.debug("Adding package " + entry.getKey());
                    packages.add(entry.getKey(), entry.getValue());
                }
            }

            root.add("packages", packages);

            String payload = gson.toJson(root);

            Content content = new Content(new StringPayload(payload, ContentTypes.TEXT_PLAIN));
            String repodataJsonPath = arch + "/repodata.json";
            String repodataJsonZstPath = arch + "/repodata.json.zst";

            put(new CondaPath(repodataJsonPath, null), content);
            log.info("Attempting to compress repodata.json to ZST format");
            compressAndSaveRepoDataJsonZst(repodataJsonPath, repodataJsonZstPath);
            log.info("Repodata.json correctly rebuild and compressed to zst format");
        }
    }
}

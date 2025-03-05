package be.kbc.eap.nexus.datastore.internal;

import be.kbc.eap.nexus.CondaFormat;
import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.CondaPathParser;
import be.kbc.eap.nexus.datastore.CondaContentFacet;
import be.kbc.eap.nexus.util.ZstdUtils;
import be.kbc.eap.nexus.util.CondaPathUtils;

import com.google.gson.*;
import org.apache.commons.lang3.StringUtils;
import org.sonatype.nexus.blobstore.api.*;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.WritePolicy;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.RepositoryContent;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.Payload;

import com.google.common.base.Preconditions;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.transaction.Transactional;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static be.kbc.eap.nexus.AssetKind.REPODATA;
import static be.kbc.eap.nexus.datastore.internal.CondaAttributesHelper.assetKind;
import static be.kbc.eap.nexus.datastore.internal.CondaAttributesHelper.setCondaAttributes;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.config.WritePolicy.ALLOW;

@Named(CondaFormat.NAME)
public class CondaContentFacetImpl extends ContentFacetSupport implements CondaContentFacet {

    private static final char ASSET_PATH_PREFIX = '/';

    private static final List<HashAlgorithm> HASH_ALGORITHMS = Collections.singletonList(HashAlgorithm.SHA1);

    private final BlobStoreManager blobStoreManager;

    private final Map<String, CondaPathParser> condaPathParsers;
    private CondaPathParser condaPathParser;

    @Inject
    public CondaContentFacetImpl(@Named(CondaFormat.NAME) FormatStoreManager formatStoreManager, BlobStoreManager blobStoreManager, Map<String, CondaPathParser> condaPathParsers) {
        super(formatStoreManager);
        this.blobStoreManager = blobStoreManager;
        this.condaPathParsers = condaPathParsers;
    }

    @Override
    protected void doInit(Configuration configuration) throws Exception {
        super.doInit(configuration);
        condaPathParser = checkNotNull(condaPathParsers.get(getRepository().getFormat().getValue()));
    }

    public Optional<FluentAsset> getAsset(String assetPath) {
        Preconditions.checkNotNull(assetPath);
        return assets().path(CondaPathUtils.normalizeAssetPath(assetPath)).find();
    }


    @Override
    public int deleteComponents(final Stream<FluentComponent> components) {
        ContentFacetSupport contentFacet = (ContentFacetSupport) facet(ContentFacet.class);
        ComponentStore<?> componentStore = contentFacet.stores().componentStore;
        List<FluentComponent> componentsList = components.collect(Collectors.toList());

        if (!ProxyType.NAME.equals(repository().getType().getValue())) {
            int deletedCount = componentStore.purge(contentFacet.contentRepositoryId(), componentsList);

            try {
                rebuildRepoDataJson();
            } catch (IOException e) {

            }
            return deletedCount;
        }
        else {
            return componentStore.purge(contentFacet.contentRepositoryId(), componentsList);
        }
    }

    @Override
    @TransactionalStoreBlob
    public Content put(final CondaPath path, final Payload content) throws IOException {
        return put(path, content, null);
    }

    @Guarded(by = STARTED)
    @Override
    public Content put(final CondaPath path, final Payload content, String indexJson) throws IOException {
        log.info("CondaFacetImpl - put - " + path.getPath() + " - " + content.getSize());

        try (TempBlob blob = blobs().ingest(content, CondaPath.HashType.ALGORITHMS)){
            return doPutContent(path, blob, content, indexJson);
        }
    }

    @Override
    public boolean delete(final CondaPath condaPath) {
        log.trace("DELETE {} : {}", getRepository().getName(), condaPath);
        boolean assetIsDeleted = deleteAsset(condaPath);
        if (assetIsDeleted && condaPath.getCoordinates() != null) {
            maybeDeleteComponent(condaPath.getCoordinates());
        }
        return assetIsDeleted;
    }

    private Optional<FluentAsset> findAsset(final String path) {
        return assets()
                .path(path)
                .find();
    }

    @Override
    public boolean delete(final List<String> paths) {
        Repository repository = getRepository();
        log.trace("DELETE {} assets at {}", repository.getName(), paths);
        return stores().assetStore.deleteAssetsByPaths(contentRepositoryId(), paths) > 0;
    }

    private boolean deleteAsset(final CondaPath condaPath) {
        return findAsset(assetPath(condaPath))
                .map(FluentAsset::delete)
                .orElse(false);
    }

    private void maybeDeleteComponent(final CondaPath.Coordinates coordinates) {
        components()
                .name(coordinates.getPackageName())
                .version(coordinates.getVersion())
                .find()
                .ifPresent(this::deleteIfNoAssetsLeft);
    }

    private void deleteIfNoAssetsLeft(final FluentComponent component) {
        if (component.assets().isEmpty()) {
            component.delete();
            //publishEvents(component);
            try {
                rebuildRepoDataJson();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //deleteMetadataOrFlagForRebuild(component);
        }
    }

    @Nonnull
    @Override
    public CondaPathParser getCondaPathParser() {
        return condaPathParser;
    }


    @Override
    protected WritePolicy writePolicy(final org.sonatype.nexus.repository.content.Asset asset) {
        WritePolicy configuredWritePolicy = super.writePolicy(asset);
        if (WritePolicy.ALLOW_ONCE == configuredWritePolicy) {
            String name = asset.kind();
            if(name == null || name.trim().equalsIgnoreCase("")) {
                name = asset.path();
                if (name.endsWith(".tar.bz2") || name.endsWith(".conda")) {
                    return WritePolicy.ALLOW_ONCE;
                }
                else {
                    return WritePolicy.ALLOW;
                }
            }
            else {
                if (StringUtils.equals(REPODATA.name(), name))
                {
                    return ALLOW;
                }
            }

        }
        return configuredWritePolicy;
    }

    @Override
    @Transactional
    public String rebuildRepoDataJson() throws IOException {

        log.info("Rebuilding repodata json/zstd");

        Map<String, List<JsonObject>> architectures = new HashMap<>();
        architectures.put("noarch", new ArrayList<>());

        // fill our list with all the components/assets and store the metadata to build our repodata.json


        this.assets().browse(Integer.MAX_VALUE, null).forEach(asset -> {
            try {
                if (!asset.path().endsWith("repodata.json") || !asset.path().endsWith("repodata.json.zst")) {
                    log.debug("Processing " + asset.path());
                    Optional<Component> optionalComponent = asset.component();
                    if (!optionalComponent.isPresent())
                        return;

                    log.debug("Fetching component");
                    Component component = optionalComponent.get();

                    if (!architectures.containsKey(component.namespace())) {
                        architectures.put(component.namespace(), new ArrayList<>());
                    }

                    log.debug("Fetching attributes");
                    List<JsonObject> artifacts = architectures.get(component.namespace());
                    NestedAttributesMap formatAttributes = asset.attributes("attributes");
                    log.debug(formatAttributes.toString());
                    formatAttributes = asset.attributes("conda");
                    log.debug(formatAttributes.toString());

                    if (!formatAttributes.contains("build_number")) {
                        return;
                    }

                    log.debug("Building artifact properties");

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
                    artifact.addProperty("size", asset.blob().get().blobSize());
                    artifact.addProperty("md5", asset.blob().get().checksums().get(HashAlgorithm.MD5.name()).toString());
                    String depends = formatAttributes.get("depends").toString();

                    JsonArray jDepends = new JsonArray();
                    if (!Strings2.isEmpty(depends)) {
                        String[] parts = depends.split(";");
                        for (String part : parts) {
                            jDepends.add(new JsonPrimitive(part));
                        }
                    }
                    artifact.add("depends", jDepends);
                    CondaPath condaPath = new CondaPath(asset.path().substring(1), null);
                    JsonObject parentArtifact = new JsonObject();
                    parentArtifact.add(condaPath.getFileName(), artifact);
                    artifacts.add(parentArtifact);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            ;
        });

        // now build the repodata.json files

        log.debug("Building json string");

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

            Content content = new Content(new StringPayload(payload, ContentTypes.APPLICATION_JSON));
            String repodataJsonPath = arch + "/repodata.json";
            String repodataJsonZstPath = arch + "/repodata.json.zst";

            put(new CondaPath(repodataJsonPath, null), content);
            log.info("Attempting to compress repodata.json to ZST format");
            compressAndSaveRepoDataJsonZst(repodataJsonPath, repodataJsonZstPath);
            log.info("Repodata.json correctly rebuild and compressed to zst format");
        }

        return "";
    }


    private void compressAndSaveRepoDataJsonZst(String sourcePath, String targetPath) throws IOException {

        if(!sourcePath.startsWith("/")) {
            sourcePath = "/" + sourcePath;
        }

        this.assets().path(sourcePath).find().ifPresent(asset -> {
            try {
                try (InputStream sourceInputStream = asset.download().getPayload().openInputStream()) {
                    byte[] compressedBytes = compressInputStream(sourceInputStream);

                    Payload payload = new BytesPayload(compressedBytes, "application/zstd");
                    put(new CondaPath(targetPath, null), payload);
                } catch (IOException e) {
                    log.info("Error compressing repodata.json to ZST format", e);
                    throw e;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
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

    protected Content doPutContent(final CondaPath path, final TempBlob blob, final Payload content, final String indexJson) {

        FluentComponent component = null;
        Optional<CondaModel> model = CondaModel.fromIndexJson(indexJson);
        if (path.getCoordinates() != null) {
            component = createOrGetComponent(path, model);
        }

        return saveAsset(path, component, content, blob, model);
    }

//    static String assetKind(final CondaPath condaPath, final CondaPathParser condaPathParser) {
//        if (condaPath.getCoordinates() != null) {
//            return artifactRelatedAssetKind(condaPath);
//        }
//        else {
//            return fileAssetKindFor(condaPath, condaPathParser);
//        }
//    }
//
//    private static String artifactRelatedAssetKind(final CondaPath condaPath) {
//        return condaPath.isSubordinate() ? ARTIFACT_SUBORDINATE.name() : ARTIFACT.name();
//    }
//
//    private static String fileAssetKindFor(final MavenPath mavenPath, final MavenPathParser mavenPathParser) {
//        if (mavenPathParser.isRepositoryMetadata(mavenPath)) {
//            return REPOSITORY_METADATA.name();
//        }
//        else if (mavenPathParser.isRepositoryIndex(mavenPath)) {
//            return REPOSITORY_INDEX.name();
//        }
//        else {
//            return OTHER.name();
//        }
//    }

    private Content saveAsset(
            final CondaPath condaPath,
            final Component component,
            final Payload content,
            final TempBlob blob,
            final Optional<CondaModel> model)
    {
        String path = assetPath(condaPath);
        FluentAssetBuilder assetBuilder = assets().path(path).kind(assetKind(condaPath, condaPathParser));
        if (component != null) {
            assetBuilder = assetBuilder.component(component);
        }

        FluentAsset asset = assetBuilder.blob(blob).save();

        if (isNewRepositoryContent(asset)) {
            setCondaAttributes(asset, condaPath, model);
        }

        return asset
                .markAsCached(content)
                .download();
    }

    private String assetPath(final CondaPath condaPath) {
        return ASSET_PATH_PREFIX + condaPath.getPath();
    }

    @Transactional
    private FluentComponent createOrGetComponent(final CondaPath condaPath, final Optional<CondaModel> model)
    {
        CondaPath.Coordinates coordinates = condaPath.getCoordinates();

        FluentComponent component = components()
                .name(coordinates.getPackageName())
                .namespace(coordinates.getNamespace())
                .version(coordinates.getVersion())
                .normalizedVersion(
                        versionNormalizerService().getNormalizedVersionByFormat(coordinates.getVersion(), repository().getFormat()))
                .getOrCreate();

        boolean isNew = isNewRepositoryContent(component);
        log.debug("Setting attributes");
        CondaAttributesHelper.setCondaAttributes(
                stores().componentStore, component, coordinates, model.orElse(null), contentRepositoryId());

//        if (isNew) {
//            // BDV: no-op
//            //publishEvents(component);
//
//        }
//        else {
//            // kind isn't set for existing components
//            optionalKind.ifPresent(component::kind);
//        }
        return component;
    }


    private boolean isNewRepositoryContent(final RepositoryContent repositoryContent) {
        return repositoryContent.attributes().isEmpty();
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

    private String getJsonAttribute(JsonObject json, String attribute, String defaultValue) {
        if(json.has(attribute) && !json.get(attribute).isJsonNull()) {
            return json.get(attribute).getAsString();
        }
        return defaultValue;
    }

//    private Content toContent(final Asset asset, final Blob blob) {
//        final Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
//        Content.extractFromAsset(asset, HASH_ALGORITHMS, content.getAttributes());
//        log.debug("Convert asset to content - Content Size: " + content.getSize() + " - Asset size: " + asset.size().toString());
//        return content;
//    }
}

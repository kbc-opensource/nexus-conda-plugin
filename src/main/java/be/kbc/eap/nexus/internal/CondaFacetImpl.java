package be.kbc.eap.nexus.internal;

import be.kbc.eap.nexus.CondaFacet;
import be.kbc.eap.nexus.CondaCoordinatesHelper;
import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.CondaPathParser;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.storage.*;
import org.sonatype.nexus.repository.transaction.*;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.transaction.UnitOfWork;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

@Named
public class CondaFacetImpl
        extends FacetSupport
        implements CondaFacet {


    private final Map<String, CondaPathParser> condaPathParsers;
    private CondaPathParser condaPathParser;

    @Inject
    public CondaFacetImpl(final Map<String, CondaPathParser> condaPathParserMap) {
        this.condaPathParsers = checkNotNull(condaPathParserMap);
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
        //log.info("CondaFacetImpl - put - " + path.getPath() + " - " + content.getSize());
        StorageFacet storageFacet = facet(StorageFacet.class);
        try (TempBlob tempBlob = storageFacet.createTempBlob(content, hashAlgorithms)) {

            log.info("call doPutContent");
            return doPutContent(path, tempBlob, content);
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


    protected Content doPutContent(final CondaPath path, final TempBlob tempBlob, final Payload payload)
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
        AssetBlob assetBlob = tx.setBlob(
                asset,
                path.getPath(),
                tempBlob,
                null,
                payload.getContentType(),
                false
        );

        tx.saveAsset(asset);

        log.info("Convert asset to content");
        return toContent(asset, assetBlob.getBlob());
    }

    @TransactionalStoreMetadata
    public Asset getOrCreateAsset(final CondaPath condaPath, final Repository repository, final String componentName, final String componentGroup,
                                  final String assetName) {
        final StorageTx tx = UnitOfWork.currentTx();

        final Bucket bucket = tx.findBucket(getRepository());
        Component component = tx.findComponentWithProperty(P_NAME, componentName, bucket);
        Asset asset;
        if (component == null) {
            // CREATE
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

            asset.name(assetName);
        } else {
            // UPDATE
            asset = tx.firstAsset(component);
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
        log.info("Convert asset to content - Content Size: " + content.getSize() + " - Asset size: " + asset.size().toString());
        return content;
    }

}

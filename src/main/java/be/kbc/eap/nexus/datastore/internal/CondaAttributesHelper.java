package be.kbc.eap.nexus.datastore.internal;

import be.kbc.eap.nexus.CondaFormat;
import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.CondaPathParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.store.ComponentStore;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static be.kbc.eap.nexus.AssetKind.*;
import static org.sonatype.nexus.repository.content.AttributeOperation.OVERLAY;

final class CondaAttributesHelper {

    private static final Log log = LogFactory.getLog(CondaAttributesHelper.class);

    private CondaAttributesHelper() {

    }

    static void setCondaAttributes(
            final ComponentStore componentStore,
            final FluentComponent component,
            final CondaPath.Coordinates coordinates,
            final CondaModel model,
            final int repositoryId)
    {
        log.debug("Setting attributes");
        Map<String, String> condaAttributes = new HashMap<>();

        condaAttributes.put("arch", coordinates.getNamespace());
        condaAttributes.put("buildString", coordinates.getBuildString());
        condaAttributes.put("version", coordinates.getVersion());
        condaAttributes.put("packageName", coordinates.getPackageName());


        if(model != null) {
            log.debug("Model present, setting attributes");
            condaAttributes.put("arch", model.getArchitecture());
            condaAttributes.put("build_number", model.getBuildNumber());
            condaAttributes.put("license", model.getLicense());
            condaAttributes.put("license_family", model.getLicenseFamily());
            condaAttributes.put("platform", model.getPlatform());
            condaAttributes.put("subdir", model.getSubdir());
            condaAttributes.put("depends", String.join(";", model.getDepends()));
        }
        log.debug("Update attributes");
        component.attributes(OVERLAY, CondaFormat.NAME, condaAttributes);
    }

    static void setCondaAttributes(final FluentAsset asset, final CondaPath condaPath, final Optional<CondaModel> model) {
        Map<String, String> condaAttributes = new HashMap<>();
        CondaPath.Coordinates coordinates = condaPath.getCoordinates();

        if(coordinates != null) {
            condaAttributes.put("arch", coordinates.getNamespace());
            condaAttributes.put("buildString", coordinates.getBuildString());
            condaAttributes.put("version", coordinates.getVersion());
            condaAttributes.put("packageName", coordinates.getPackageName());
        }

        if (coordinates != null && model.isPresent()) {
            condaAttributes.put("arch", model.get().getArchitecture());
            condaAttributes.put("build_number", model.get().getBuildNumber());
            condaAttributes.put("license", model.get().getLicense());
            condaAttributes.put("license_family", model.get().getLicenseFamily());
            condaAttributes.put("platform", model.get().getPlatform());
            condaAttributes.put("subdir", model.get().getSubdir());
            condaAttributes.put("depends", String.join(";", model.get().getDepends()));

        }
        asset.attributes(OVERLAY, CondaFormat.NAME, condaAttributes);
    }

    static String assetKind(final CondaPath condaPath, final CondaPathParser condaPathParser) {
        if (condaPath.getCoordinates() != null) {
            return artifactRelatedAssetKind(condaPath);
        }
        else {
            return fileAssetKindFor(condaPath, condaPathParser);
        }
    }

    private static String artifactRelatedAssetKind(final CondaPath condaPath) {
        return ARTIFACT.name();
    }

    private static String fileAssetKindFor(final CondaPath conda, final CondaPathParser condaPathParser) {
        if (condaPathParser.isRepodata(conda) || condaPathParser.isRepodataZst(conda)) {
            return REPODATA.name();
        }
        else {
            return OTHER.name();
        }
    }
}

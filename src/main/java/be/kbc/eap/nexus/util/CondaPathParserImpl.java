package be.kbc.eap.nexus.util;

import be.kbc.eap.nexus.CondaFormat;
import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.CondaPathParser;
import com.google.common.annotations.VisibleForTesting;
import org.sonatype.goodies.common.ComponentSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
@Named(CondaFormat.NAME)
public class CondaPathParserImpl
        extends ComponentSupport
        implements CondaPathParser {


    @Nonnull
    @Override
    public CondaPath parsePath(String path) {
        return parsePath(path, true);
    }

    @Nonnull
    @Override
    public CondaPath parsePath(String path, boolean caseSensitive) {
        checkNotNull(path);
        String pathWithoutLeadingSlash = path;
        if (path.startsWith("/")) {
            pathWithoutLeadingSlash = path.substring(1);
        }
        try {
            final CondaPath.Coordinates coordinates = condaPathToCoordinates(pathWithoutLeadingSlash, caseSensitive);
            return new CondaPath(pathWithoutLeadingSlash, coordinates);
        }
        catch (Exception e) {
            return new CondaPath(pathWithoutLeadingSlash, null);
        }


    }

    @Override
    public boolean isRepodata(CondaPath path) {
        return path.main().getFileName().equalsIgnoreCase(Constants.REPODATA_JSON);
    }

    @Override
    public boolean isRepodataZst(CondaPath path) {
        return path.main().getFileName().equalsIgnoreCase(Constants.REPODATA_JSON_ZST);
    }

    @Nullable
    @VisibleForTesting
    CondaPath.Coordinates condaPathToCoordinates(final String pathString, final boolean caseSensitive) {
        String str = pathString;

        String[] pathParts = str.split("/");

        if(pathParts.length == 0) {
            return null;
        }

        String namespace = pathParts[0];


        if(str.endsWith(Constants.REPODATA_JSON) || str.endsWith(Constants.REPODATA_JSON_ZST)) {
            return null;
        }

        int vEndPos = str.lastIndexOf('/');
        if (vEndPos == -1) {
            return null;
        }

        final String fileName = str.substring(vEndPos + 1);

        String[] parts = fileName.split("-");

        String buildAndExtension = parts[parts.length - 1];
        String extension = "";
        if(buildAndExtension.endsWith("tar.bz2")) {
            extension = "tar.bz2";
            buildAndExtension = buildAndExtension.replace(".tar.bz2", "");
        }
        else if(buildAndExtension.endsWith("conda")) {
            extension = "conda";
            buildAndExtension = buildAndExtension.replace(".conda", "");
        }

        String build = buildAndExtension;

        String version = parts[parts.length - 2];
        String packageName = "";

        for(int i = 0; i < parts.length - 2; i++) {
            packageName += parts[i];
            if(i < parts.length - 3) {
                packageName += "-";
            }
        }



        return new CondaPath.Coordinates(namespace, packageName, version, build, extension);
    }

}

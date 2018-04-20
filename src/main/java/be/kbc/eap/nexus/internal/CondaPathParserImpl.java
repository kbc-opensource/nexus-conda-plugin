package be.kbc.eap.nexus.internal;

import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.CondaPathParser;
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
        final CondaPath.Coordinates coordinates = condaPathToCoordinates(pathWithoutLeadingSlash, caseSensitive);
        return new CondaPath(pathWithoutLeadingSlash, coordinates);

    }

    @Override
    public boolean isRepodata(CondaPath path) {
        return path.main().getFileName().equalsIgnoreCase(Constants.REPODATA_JSON);
    }

    @Nullable
    private CondaPath.Coordinates condaPathToCoordinates(final String pathString, final boolean caseSensitive) {
        String str = pathString;

        if(str.endsWith(Constants.REPODATA_JSON)) {
            return null;
        }

        int vEndPos = str.lastIndexOf('/');
        if (vEndPos == -1) {
            return null;
        }

        final String fileName = str.substring(vEndPos + 1);

        String[] parts = fileName.split("-");

        String packageName = parts[0];
        String version = "";
        String build = "";
        String extension = "";
        if(parts.length>1) {
            version = parts[1];
            build = parts[2].replace(".tar.bz2", "");
        }

        int nExtPos = fileName.lastIndexOf('.');
        if(fileName.endsWith(".tar.bz2")) {
            nExtPos -= 4;
        }

        extension = fileName.substring(nExtPos + 1);

        return new CondaPath.Coordinates(packageName, version, build, extension);
    }

}

package be.kbc.eap.nexus.util;

import org.apache.commons.lang3.StringUtils;

public class CondaPathUtils {

    public static String normalizeAssetPath(String path) {
        return StringUtils.prependIfMissing(path, "/", new CharSequence[0]);
    }

}

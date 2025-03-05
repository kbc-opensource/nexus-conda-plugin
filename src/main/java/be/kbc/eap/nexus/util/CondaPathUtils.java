package be.kbc.eap.nexus.util;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import be.kbc.eap.nexus.AssetKind;

public class CondaPathUtils {

    public static String name(TokenMatcher.State state) {
        return match(state, "name");
    }

    public static String version(TokenMatcher.State state) {
        return match(state, "version");
    }


    private static String match(TokenMatcher.State state, String name) {
        Preconditions.checkNotNull(state);
        String result = (String)state.getTokens().get(name);
        Preconditions.checkNotNull(result);
        return result;
    }

    public static String normalizeAssetPath(String path) {
        return StringUtils.prependIfMissing(path, "/", new CharSequence[0]);
    }

}

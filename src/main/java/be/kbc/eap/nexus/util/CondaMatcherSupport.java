package be.kbc.eap.nexus.util;

import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.CondaPathParser;
import com.google.common.base.Predicate;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Matcher;

import static com.google.common.base.Preconditions.checkNotNull;

public class CondaMatcherSupport
    extends ComponentSupport
    implements Matcher {

    public static Predicate<String> withHashes(final Predicate<String> predicate) {
        return (String input) ->
        {
            String mainPath = input;
            for (CondaPath.HashType hashType : CondaPath.HashType.values()) {
                if (mainPath.endsWith("." + hashType.getExt())) {
                    mainPath = mainPath.substring(0, mainPath.length() - (hashType.getExt().length() + 1));
                    break;
                }
            }
            return predicate.apply(mainPath);
        };
    }

    private final CondaPathParser condaPathParser;

    private final Predicate<String> predicate;

    public CondaMatcherSupport(final CondaPathParser condaPathParser, final Predicate<String> predicate) {
        this.condaPathParser = checkNotNull(condaPathParser);
        this.predicate = checkNotNull(predicate);
    }

    @Override
    public boolean matches(Context context) {
        final String path = context.getRequest().getPath();
        if (predicate.apply(path)) {
            final CondaPath condaPath = condaPathParser.parsePath(path);
            context.getAttributes().set(CondaPath.class, condaPath);
            return true;
        }
        return false;
    }
}

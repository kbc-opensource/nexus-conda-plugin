package be.kbc.eap.nexus;

import javax.annotation.Nonnull;

public interface CondaPathParser {
    /**
     * Parses path into {@link CondaPath}.
     */
    @Nonnull
    CondaPath parsePath(String path);

    /**
     * Parses path into {@link CondaPath} with optional case sensitivity
     *
     * @since 3.7
     */
    @Nonnull
    CondaPath parsePath(String path, boolean caseSensitive);

    /**
     * Returns {@code true} if passed in path represent repository data json.
     */
    boolean isRepodata(CondaPath path);
}

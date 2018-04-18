package be.kbc.eap.nexus;

import com.google.common.collect.ImmutableList;
import org.sonatype.nexus.common.hash.HashAlgorithm;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class CondaPath {

    public enum HashType
    {
        SHA1("sha1", HashAlgorithm.SHA1),

        MD5("md5", HashAlgorithm.MD5);

        /**
         * {@link HashAlgorithm}s corresponding to {@link HashType}s.
         */
        public static final List<HashAlgorithm> ALGORITHMS = ImmutableList
                .of(SHA1.getHashAlgorithm(), MD5.getHashAlgorithm());

        private final String ext;

        private final HashAlgorithm hashAlgorithm;

        HashType(final String ext, final HashAlgorithm hashAlgorithm) {
            this.ext = ext;
            this.hashAlgorithm = hashAlgorithm;
        }

        public String getExt() {
            return ext;
        }

        public HashAlgorithm getHashAlgorithm() {
            return hashAlgorithm;
        }
    }



    public static class Coordinates
    {


        private final String packageName;

        private final String version;

        private final String buildString;

        private final String extension;


        public Coordinates(final String packageName,
                           final String version,
                           final String buildString,
                           final String extension)

        {
            this.packageName = checkNotNull(packageName);
            this.version = checkNotNull(version);
            this.buildString = checkNotNull(buildString);
            this.extension = checkNotNull(extension);
        }

        @Nonnull
        public String getPackageName() {
            return packageName;
        }

        @Nonnull
        public String getVersion() {
            return version;
        }

        @Nonnull
        public String getBuildString() {
            return buildString;
        }

        @Nonnull
        public String getExtension() {
            return extension;
        }
    }


    private final String path;

    private final String fileName;

    private final HashType hashType;

    private final Coordinates coordinates;

    public CondaPath(final String path, final Coordinates coordinates)
    {
        checkNotNull(path);
        checkArgument(!path.startsWith("/"), "Path must not start with '/'");
        this.path = path;
        this.fileName = this.path.substring(path.lastIndexOf('/') + 1);
        HashType ht = null;
        for (HashType v : HashType.values()) {
            if (this.fileName.endsWith("." + v.getExt())) {
                ht = v;
                break;
            }
        }
        this.hashType = ht;
        this.coordinates = coordinates;
    }

    @Nonnull
    public String getPath() {
        return path;
    }

    @Nonnull
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns hash type if this path points at Maven hash file, otherwise {@code null}.
     */
    @Nullable
    public HashType getHashType() {
        return hashType;
    }

    /**
     * Returns the Maven coordinates if this path is an artifact path, otherwise {@code null}.
     */
    @Nullable
    public Coordinates getCoordinates() {
        return coordinates;
    }

    /**
     * Returns {@code true} if this path is subordinate (is hash or signature) of another path.
     *
     * @see {@link #subordinateOf()}
     */
    public boolean isSubordinate() {
        return isHash();
    }

    /**
     * Returns {@code true} if this path represents a hash.
     */
    public boolean isHash() {
        return hashType != null;
    }


    /**
     * Returns the "main", non-subordinate path of this path. The "main" path is never a hash nor a signature.
     */
    @Nonnull
    public CondaPath main() {
        CondaPath condaPath = this;
        while (condaPath.isSubordinate()) {
            condaPath = condaPath.subordinateOf();
        }
        return condaPath;
    }

    /**
     * Returns the "parent" path, that this path is subordinate of, or this instance if it is not a subordinate.
     */
    @Nonnull
    public CondaPath subordinateOf() {
        if (hashType != null) {
            int hashSuffixLen = hashType.getExt().length() + 1; // the dot
            Coordinates mainCoordinates = null;
            if (coordinates != null) {
                mainCoordinates = new Coordinates(
                        coordinates.getPackageName(),
                        coordinates.getVersion(),
                        coordinates.getBuildString(),
                        coordinates.getExtension()
                );
            }
            return new CondaPath(
                    path.substring(0, path.length() - hashSuffixLen),
                    mainCoordinates
            );
        }
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CondaPath)) {
            return false;
        }
        CondaPath that = (CondaPath) o;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "path='" + path + '\'' +
                ", fileName='" + fileName + '\'' +
                ", hashType=" + hashType +
                '}';
    }


    /**
     * Returns path of passed in hash type that is subordinate of this path. This path cannot be hash.
     */
    @Nonnull
    public CondaPath hash(final HashType hashType) {
        checkNotNull(hashType);
        checkArgument(this.hashType == null, "This path is already a hash: %s", this);
        Coordinates hashCoordinates = null;
        if (coordinates != null) {
            hashCoordinates = new Coordinates(
                    coordinates.getPackageName(),
                    coordinates.getVersion(),
                    coordinates.getBuildString(),
                    coordinates.getExtension()
            );
        }
        return new CondaPath(
                path + "." + hashType.getExt(),
                hashCoordinates
        );
    }

}

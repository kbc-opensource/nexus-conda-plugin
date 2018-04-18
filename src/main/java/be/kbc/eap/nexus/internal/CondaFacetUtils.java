package be.kbc.eap.nexus.internal;

import be.kbc.eap.nexus.CondaFacet;
import be.kbc.eap.nexus.CondaPath;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashingOutputStream;
import org.joda.time.DateTime;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import javax.annotation.Nonnull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class CondaFacetUtils {

    /**
     * Wrapper to pass in into {@link #createTempContent(Path, String, Writer)} to write out actual content.
     */
    public interface Writer
    {
        void write(OutputStream outputStream) throws IOException;
    }

    public static Content createTempContent(final Path path, final String contentType, final Writer writer) throws IOException {
        Map<HashAlgorithm, HashingOutputStream> hashingStreams = new HashMap<>();
        try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(path))) {
            OutputStream os = outputStream;
            for (CondaPath.HashType hashType : CondaPath.HashType.values()) {
                os = new HashingOutputStream(hashType.getHashAlgorithm().function(), os);
                hashingStreams.put(hashType.getHashAlgorithm(), (HashingOutputStream) os);
            }
            writer.write(os);
            os.flush();
        }
        Map<HashAlgorithm, HashCode> hashCodes = new HashMap<>();
        for (Map.Entry<HashAlgorithm, HashingOutputStream> entry : hashingStreams.entrySet()) {
            hashCodes.put(entry.getKey(), entry.getValue().hash());
        }
        Content content = new Content(new StreamPayload(
                new StreamPayload.InputStreamSupplier()
                {
                    @Nonnull
                    @Override
                    public InputStream get() throws IOException {
                        return new BufferedInputStream(Files.newInputStream(path));
                    }
                },
                Files.size(path),
                contentType)
        );
        content.getAttributes().set(Content.CONTENT_LAST_MODIFIED, DateTime.now());
        content.getAttributes().set(Content.CONTENT_HASH_CODES_MAP, hashCodes);
        mayAddETag(content);
        return content;
    }

    /**
     * Adds {@link Content#CONTENT_ETAG} content attribute if not present. In case of hosted repositories, this is safe
     * and even good thing to do, as the content is hosted here only and NX is content authority.
     */
    public static void mayAddETag(final Content content) {
        if (content.getAttributes().contains(Content.CONTENT_ETAG)) {
            return;
        }
        Map<HashAlgorithm, HashCode> hashCodes = content.getAttributes()
                .require(Content.CONTENT_HASH_CODES_MAP, Content.T_CONTENT_HASH_CODES_MAP);
        HashCode sha1HashCode = hashCodes.get(HashAlgorithm.SHA1);
        if (sha1HashCode != null) {
            content.getAttributes().set(Content.CONTENT_ETAG, "{SHA1{" + sha1HashCode + "}}");
        }
    }

    /**
     * Performs a {@link CondaFacet#put(CondaPath, Payload)} for passed in {@link Content} and it's hashes too. Returns
     * the put content.
     */
    public static Content putWithHashes(final CondaFacet condaFacet,
                                        final CondaPath condaPath,
                                        final Content content) throws IOException
    {
        final Map<HashAlgorithm, HashCode> hashCodes = content.getAttributes().require(
                Content.CONTENT_HASH_CODES_MAP, Content.T_CONTENT_HASH_CODES_MAP);
        final DateTime now = content.getAttributes().require(Content.CONTENT_LAST_MODIFIED, DateTime.class);
        Content result = condaFacet.put(condaPath, content);
        for (CondaPath.HashType hashType : CondaPath.HashType.values()) {
            final HashCode hashCode = hashCodes.get(hashType.getHashAlgorithm());
            if (hashCode != null) {
                final Content hashContent = new Content(
                        new StringPayload(hashCode.toString(), Constants.CHECKSUM_CONTENT_TYPE));
                hashContent.getAttributes().set(Content.CONTENT_LAST_MODIFIED, now);
                condaFacet.put(condaPath.hash(hashType), hashContent);
            }
        }
        return result;
    }

    /**
     * Performs a {@link CondaFacet#delete(CondaPath...)} for passed in {@link CondaPath} and all it's hashes too.
     * Returns {@code true} if any of deleted paths existed.
     */
    public static boolean deleteWithHashes(final CondaFacet condaFacet, final CondaPath condaPath) throws IOException {
        final ArrayList<CondaPath> paths = new ArrayList<>(CondaPath.HashType.values().length + 1);
        paths.add(condaPath.main());
        for (CondaPath.HashType hashType : CondaPath.HashType.values()) {
            paths.add(condaPath.main().hash(hashType));
        }
        return condaFacet.delete(paths.toArray(new CondaPath[paths.size()]));
    }
}

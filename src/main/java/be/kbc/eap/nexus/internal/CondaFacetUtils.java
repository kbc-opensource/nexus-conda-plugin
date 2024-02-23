package be.kbc.eap.nexus.internal;

import be.kbc.eap.nexus.CondaCoordinatesHelper;
import be.kbc.eap.nexus.CondaFacet;
import be.kbc.eap.nexus.CondaPath;
import com.github.luben.zstd.ZstdOutputStream;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashingOutputStream;
import org.joda.time.DateTime;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.io.InputStreamSupplier;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.*;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;


public class CondaFacetUtils {

    /**
     * Wrapper to pass in into {@link #createTempContent(Path, String, Writer)} to write out actual content.
     */
    public interface Writer {
        void write(OutputStream outputStream) throws IOException;
    }

    @Nullable
    public static Component findComponent(final StorageTx tx,
                                          final Repository repository,
                                          final CondaPath condaPath)
    {
        final CondaPath.Coordinates coordinates = condaPath.getCoordinates();
        Query query = null;
        if(coordinates == null) {

            query = Query.builder()
                    .where(P_GROUP).eq(CondaCoordinatesHelper.getGroup(condaPath.getPath()))
                    .and(P_NAME).eq(condaPath.getFileName())
                    .build();

        }
        else {
            query = Query.builder()
                    .where(P_GROUP).eq(CondaCoordinatesHelper.getGroup(condaPath.getPath()))
                    .and(P_NAME).eq(coordinates.getPackageName())
                    .and(P_VERSION).eq(coordinates.getVersion())
                    .build();

        }

        final Iterable<Component> components = tx.findComponents(
                query,
                singletonList(repository)
        );
        if (components.iterator().hasNext()) {
            return components.iterator().next();
        }
        return null;
    }

    /**
     * 103    * Finds asset in given bucket by key.
     * 104
     */
    @Nullable
    public static Asset findAsset(final StorageTx tx,
                                  final Bucket bucket,
                                  final CondaPath mavenPath) {
        // The conda path is stored in the asset 'name' field, which is indexed (the maven format-specific key is not).
        return tx.findAssetWithProperty(P_NAME, mavenPath.getPath(), bucket);
    }

    public static Map<HashAlgorithm, HashCode> getHashAlgorithmFromContent(final AttributesMap attributesMap) {
        return attributesMap
                .require(Content.CONTENT_HASH_CODES_MAP, Content.T_CONTENT_HASH_CODES_MAP);
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
                new InputStreamSupplier() {
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

//    /**
//     * Compresses the given data with Zstandard (.zst) and creates a Content object.
//     *
//     * @param data     The data to compress and write.
//     * @param mimeType The MIME type of the content.
//     * @return Content The Content object representing the compressed file.
//     * @throws IOException if an I/O error occurs.
//     */
//    public static Content createZstdContent(byte[] data, String mimeType) throws IOException {
//        // Compress data using Zstandard
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        try (ZstdOutputStream zos = new ZstdOutputStream(byteArrayOutputStream)) {
//            zos.write(data);
//        }
//        byte[] compressedData = byteArrayOutputStream.toByteArray();
//
//        Content content = getContent(mimeType, compressedData);
//
//        AttributesMap attributes = content.getAttributes();
//        attributes.set(Content.CONTENT_LAST_MODIFIED, System.currentTimeMillis());
//
//        return content;
//    }

    private static Content getContent(String mimeType, byte[] compressedData) {
        Payload payload = new Payload() {
            @Override
            public InputStream openInputStream() {
                return new ByteArrayInputStream(compressedData);
            }

            @Override
            public long getSize() {
                return compressedData.length;
            }

            @Nullable
            @Override
            public String getContentType() {
                return mimeType;
            }

            @Override
            public void close() {
                // No action needed for ByteArrayInputStream
            }
        };

        return new Content(payload);
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
                                        final Content content) throws IOException {
        final Map<HashAlgorithm, HashCode> hashCodes = content.getAttributes().require(
                Content.CONTENT_HASH_CODES_MAP, Content.T_CONTENT_HASH_CODES_MAP);
        final DateTime now = content.getAttributes().require(Content.CONTENT_LAST_MODIFIED, DateTime.class);
        Content result = condaFacet.put(condaPath, content);
        addHashes(condaFacet, condaPath, hashCodes, DateTime.now());
        return result;
    }

    public static void addHashes(final CondaFacet condaFacet,
                                 final CondaPath condaPath,
                                 final Map<HashAlgorithm, HashCode> hashCodes,
                                 final DateTime now)
            throws IOException
    {
        for (CondaPath.HashType hashType : CondaPath.HashType.values()) {
            final HashCode hashCode = hashCodes.get(hashType.getHashAlgorithm());
            if (hashCode != null) {
                final Content hashContent = new Content(
                        new StringPayload(hashCode.toString(), Constants.CHECKSUM_CONTENT_TYPE));
                hashContent.getAttributes().set(Content.CONTENT_LAST_MODIFIED, now);
                condaFacet.put(condaPath.hash(hashType), hashContent);
            }
        }
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

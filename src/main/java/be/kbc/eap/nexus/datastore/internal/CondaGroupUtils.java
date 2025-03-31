package be.kbc.eap.nexus.datastore.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.luben.zstd.ZstdInputStream;
import com.google.common.base.Suppliers;
import org.slf4j.Logger;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.view.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;


public class CondaGroupUtils {

    private CondaGroupUtils() {
    }

    public static Supplier<String> lazyMergeResult(final Repository repository, final Map<Repository, Response> remoteResponses, final boolean isZstd, final Logger log) {
        log.debug(repository.getName() + " - Merging repodata content");
        return Suppliers.memoize(() -> mergeResponses(repository, remoteResponses, isZstd, log));
    }
    private static String mergeResponses(final Repository repository, final Map<Repository, Response> remoteResponses, boolean isZstd, final Logger log) {
        final List<Map.Entry<Repository, Response>> successfulResponses = new ArrayList<>();
        for (final Map.Entry<Repository, Response> entry : remoteResponses.entrySet()) {
            if (entry.getValue().getStatus().getCode() == 200 && entry.getValue().getPayload() != null) {
                successfulResponses.add(entry);
            }
            else {
                log.debug("Skipping response from {} with status code {}", entry.getKey().getName(), entry.getValue().getStatus().getCode());
            }
        }
        if (successfulResponses.isEmpty()) {
            log.warn("No successful responses to merge");
            return null;
        }
        return processResults(repository, successfulResponses, isZstd, log);
    }
    private static String processResults(final Repository repository, final List<Map.Entry<Repository, Response>> responses, final boolean isZstd, final Logger log) {

        log.debug(repository.getName() + " - Processing results for {} repositories", responses.size());
        ObjectMapper mapper = new ObjectMapper();
        try {

            InputStream inputStream = null;

            Map.Entry<Repository, Response> entry = responses.get(responses.size() - 1);

            log.debug(repository.getName() + " - Processing response from {}", entry.getKey().getName());

            if(isZstd) {
                inputStream = new ZstdInputStream(entry.getValue().getPayload().openInputStream());
            }
            else {
                inputStream = entry.getValue().getPayload().openInputStream();
            }

            JsonNode node = mapper.readTree(inputStream);
            ObjectNode packages = (ObjectNode) node.get("packages");

            inputStream.close();

            for (int i = responses.size() - 2; i >= 0; i--) {
                entry = responses.get(i);
                log.debug(repository.getName() + " - Processing response from {}", entry.getKey().getName());
                if(isZstd) {
                    inputStream = new ZstdInputStream(entry.getValue().getPayload().openInputStream());
                }
                else {
                    inputStream = entry.getValue().getPayload().openInputStream();
                }
                JsonNode otherNode = mapper.readTree(inputStream);
                inputStream.close();
                //log.debug("Merging repodata content");
                JsonNode otherPackages = otherNode.get("packages");
                otherPackages.fields().forEachRemaining(pack -> {
                    log.trace("Adding package {}", pack.getKey());
                    packages.set(pack.getKey(), pack.getValue());
                });
            }

            return node.toString();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}

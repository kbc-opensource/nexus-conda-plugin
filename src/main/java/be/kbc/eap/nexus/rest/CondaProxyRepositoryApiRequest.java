package be.kbc.eap.nexus.rest;

import be.kbc.eap.nexus.internal.CondaFormat;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonatype.nexus.repository.rest.api.model.CleanupPolicyAttributes;
import org.sonatype.nexus.repository.rest.api.model.HttpClientAttributes;
import org.sonatype.nexus.repository.rest.api.model.NegativeCacheAttributes;
import org.sonatype.nexus.repository.rest.api.model.ProxyAttributes;
import org.sonatype.nexus.repository.rest.api.model.ProxyRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.api.model.ReplicationAttributes;
import org.sonatype.nexus.repository.rest.api.model.StorageAttributes;

@JsonIgnoreProperties({"format", "type"})
public class CondaProxyRepositoryApiRequest extends ProxyRepositoryApiRequest {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CondaProxyRepositoryApiRequest(@JsonProperty("name") String name,
                                          @JsonProperty("online") Boolean online,
                                          @JsonProperty("storage") StorageAttributes storage,
                                          @JsonProperty("cleanup") CleanupPolicyAttributes cleanup,
                                          @JsonProperty("proxy") ProxyAttributes proxy,
                                          @JsonProperty("negativeCache") NegativeCacheAttributes negativeCache,
                                          @JsonProperty("httpClient") HttpClientAttributes httpClient,
                                          @JsonProperty("routingRule") String routingRule,
                                          @JsonProperty("replication") @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_NULL) ReplicationAttributes replication
    ) {
        super(name, CondaFormat.NAME, online, storage, cleanup, proxy, negativeCache, httpClient, routingRule, replication);
    }
}


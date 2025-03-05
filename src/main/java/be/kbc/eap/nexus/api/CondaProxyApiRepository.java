package be.kbc.eap.nexus.api;

import be.kbc.eap.nexus.CondaFormat;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonatype.nexus.repository.rest.api.model.*;

import javax.validation.constraints.NotNull;

public class CondaProxyApiRepository extends SimpleApiProxyRepository {
    @NotNull
    protected final CondaProxyRepositoriesAttributes conda;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CondaProxyApiRepository(
            @JsonProperty("name") final String name,
            @JsonProperty("url") final String url,
            @JsonProperty("online") final Boolean online,
            @JsonProperty("storage") final StorageAttributes storage,
            @JsonProperty("cleanup") final CleanupPolicyAttributes cleanup,
            @JsonProperty("conda") final CondaProxyRepositoriesAttributes conda,
            @JsonProperty("proxy") final ProxyAttributes proxy,
            @JsonProperty("negativeCache") final NegativeCacheAttributes negativeCache,
            @JsonProperty("httpClient") final HttpClientAttributes httpClient,
            @JsonProperty("routingRuleName") final String routingRuleName,
            @JsonProperty("replication") @JsonInclude(value= JsonInclude.Include.NON_EMPTY, content= JsonInclude.Include.NON_NULL)
            final ReplicationAttributes replication)
    {
        super(name, CondaFormat.NAME, url, online, storage, cleanup, proxy, negativeCache, httpClient, routingRuleName,
                replication);
        this.conda = conda;
    }

    public CondaProxyRepositoriesAttributes getConda() {
        return conda;
    }

}

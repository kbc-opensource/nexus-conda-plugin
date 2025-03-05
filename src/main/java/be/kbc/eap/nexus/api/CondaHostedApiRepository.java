package be.kbc.eap.nexus.api;

import be.kbc.eap.nexus.CondaFormat;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonatype.nexus.repository.rest.api.model.*;

import javax.validation.constraints.NotNull;

public class CondaHostedApiRepository extends SimpleApiHostedRepository {
    @NotNull
    protected final CondaHostedRepositoriesAttributes conda;

    @JsonCreator
    public CondaHostedApiRepository(
            @JsonProperty("name") final String name,
            @JsonProperty("url") final String url,
            @JsonProperty("online") final Boolean online,
            @JsonProperty("storage") final HostedStorageAttributes storage,
            @JsonProperty("cleanup") final CleanupPolicyAttributes cleanup,
            @JsonProperty("conda") final CondaHostedRepositoriesAttributes conda,
            @JsonProperty("component") final ComponentAttributes component) {
        super(name, CondaFormat.NAME, url, online, storage, cleanup, component);
        this.conda = conda;
    }

    public CondaHostedRepositoriesAttributes getConda() {
        return conda;
    }

}

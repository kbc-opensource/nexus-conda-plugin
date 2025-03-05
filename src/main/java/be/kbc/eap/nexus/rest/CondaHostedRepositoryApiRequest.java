package be.kbc.eap.nexus.rest;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import be.kbc.eap.nexus.CondaFormat;
import be.kbc.eap.nexus.api.CondaHostedRepositoriesAttributes;
import org.sonatype.nexus.repository.rest.api.model.ComponentAttributes;
import org.sonatype.nexus.repository.rest.api.model.HostedRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.api.model.CleanupPolicyAttributes;
import org.sonatype.nexus.repository.rest.api.model.HostedStorageAttributes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @since 3.20
 */
@JsonIgnoreProperties({"format", "type"})
public class CondaHostedRepositoryApiRequest
        extends HostedRepositoryApiRequest
{
    @NotNull
    @Valid
    protected final CondaHostedRepositoriesAttributes conda;

    @JsonCreator
    public CondaHostedRepositoryApiRequest(
            @JsonProperty("name") final String name,
            @JsonProperty("online") final Boolean online,
            @JsonProperty("storage") final HostedStorageAttributes storage,
            @JsonProperty("cleanup") final CleanupPolicyAttributes cleanup,
            @JsonProperty("conda") final CondaHostedRepositoriesAttributes conda,
            @JsonProperty("component") final ComponentAttributes componentAttributes)
    {
        super(name, CondaFormat.NAME, online, storage, cleanup, componentAttributes);
        this.conda = conda;
    }

    public CondaHostedRepositoriesAttributes getConda() {
        return conda;
    }

}

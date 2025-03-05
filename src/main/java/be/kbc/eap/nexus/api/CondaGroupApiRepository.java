package be.kbc.eap.nexus.api;

import be.kbc.eap.nexus.CondaFormat;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonatype.nexus.repository.rest.api.model.*;

import javax.validation.constraints.NotNull;

public class CondaGroupApiRepository extends SimpleApiGroupRepository {
    @NotNull
    protected final CondaGroupRepositoriesAttributes conda;

    @JsonCreator
    public CondaGroupApiRepository(
            @JsonProperty("name") final String name,
            @JsonProperty("url") final String url,
            @JsonProperty("online") final Boolean online,
            @JsonProperty("storage") final HostedStorageAttributes storage,
            @JsonProperty("conda") final CondaGroupRepositoriesAttributes conda,
            @JsonProperty("component") final GroupAttributes group)
    {
        super(name,
                CondaFormat.NAME,
                url,
                online,
                storage,
                group);
        this.conda = conda;
    }

    public CondaGroupRepositoriesAttributes getConda() {
        return conda;
    }

}

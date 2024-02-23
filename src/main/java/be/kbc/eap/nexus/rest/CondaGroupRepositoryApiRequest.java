package be.kbc.eap.nexus.rest;

import be.kbc.eap.nexus.internal.CondaFormat;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonatype.nexus.repository.rest.api.model.GroupAttributes;
import org.sonatype.nexus.repository.rest.api.model.GroupRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.api.model.StorageAttributes;


@JsonIgnoreProperties({"format", "type"})
public class CondaGroupRepositoryApiRequest extends GroupRepositoryApiRequest {
    @JsonCreator
    public CondaGroupRepositoryApiRequest(
            @JsonProperty("name") final String name,
            @JsonProperty("online") final Boolean online,
            @JsonProperty("storage") final StorageAttributes storage,
            @JsonProperty("group") final GroupAttributes group) {
        super(name, CondaFormat.NAME, online, storage, group);
    }

}

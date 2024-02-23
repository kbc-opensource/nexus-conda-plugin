package be.kbc.eap.nexus.internal.api;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotEmpty;

/**
 * Not used atm, as there are no specific properties. May be used in the future.
 */

@JsonSerialize // needed to avoid "fail on empty beans" error: https://stackoverflow.com/questions/15261456/how-do-i-disable-fail-on-empty-beans-in-jackson
public class CondaProxyRepositoriesAttributes extends CondaHostedRepositoriesAttributes {

// Example from APT plugin
//
//    @ApiModelProperty(value = "Distribution to fetch", example = "bionic")
//    @NotEmpty
//    protected final String distribution;
//
//    @JsonCreator
//    public AptHostedRepositoriesAttributes(@JsonProperty("distribution") final String distribution) {
//        this.distribution = distribution;
//    }
//
//    public String getDistribution() {
//        return distribution;
//    }
}

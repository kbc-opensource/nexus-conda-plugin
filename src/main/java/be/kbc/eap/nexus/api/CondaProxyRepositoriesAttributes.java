package be.kbc.eap.nexus.api;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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

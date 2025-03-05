package be.kbc.eap.nexus.api;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Not used atm, as there are no specific properties. May be used in the future.
 */

@JsonSerialize // needed to avoid "fail on empty beans" error: https://stackoverflow.com/questions/15261456/how-do-i-disable-fail-on-empty-beans-in-jackson
public class CondaGroupRepositoriesAttributes {
}

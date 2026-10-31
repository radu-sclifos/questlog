package com.radusclifos.questlog.integration.igdb;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Holds the IGDB_ID and name of an item linked to a Game (ex: platform , company).
 * The resolver uses these values to find or create the matching local entity.
 * GameType names arrive in a field called "type".
 */
public record IgdbReference(Long id, @JsonAlias("type") String name) {

}

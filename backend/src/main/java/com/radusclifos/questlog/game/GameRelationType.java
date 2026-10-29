package com.radusclifos.questlog.game;

/**
 * Directed links from source to target: an original points to its DLC, remake, remaster, ...
 * PARENT_GAME and VERSION_OF point to the base game.
 */
public enum GameRelationType {

    PARENT_GAME,
    VERSION_OF,
    DLC,
    EXPANSION,
    STANDALONE_EXPANSION,
    REMAKE,
    REMASTER,
    PORT,
    EXPANDED_GAME,
    FORK

}

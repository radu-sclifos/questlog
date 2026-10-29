package com.radusclifos.questlog.batch.catalog;

import com.radusclifos.questlog.game.Company;
import com.radusclifos.questlog.game.Engine;
import com.radusclifos.questlog.game.Franchise;
import com.radusclifos.questlog.game.Platform;

import java.util.Map;

/**
 * Maps belong only to the current page transaction, never to the singleton resolver.
 */
public record PageReferences(Map<Long, Platform> platforms, Map<Long, Engine> engines,
                             Map<Long, Franchise> franchises, Map<Long, Company> companies) {
}

package com.radusclifos.questlog.game.repository;

import com.radusclifos.questlog.game.Game;
import com.radusclifos.questlog.shared.search.SearchRepository;

import java.util.Optional;


public interface GameRepository extends SearchRepository<Game, Long> {

    Optional<Game> findByIgdbId(Long igdbId);

}

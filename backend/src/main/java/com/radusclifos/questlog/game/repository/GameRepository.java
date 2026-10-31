package com.radusclifos.questlog.game.repository;

import com.radusclifos.questlog.game.Game;
import com.radusclifos.questlog.game.dto.GameSuggestion;
import com.radusclifos.questlog.shared.search.SearchRepository;

import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface GameRepository extends SearchRepository<Game, Long> {

    List<GameSuggestion> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Optional<Game> findByIgdbId(Long igdbId);

    List<Game> findAllByIgdbIdIn(Collection<Long> igdbIds);

}

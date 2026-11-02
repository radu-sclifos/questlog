package com.radusclifos.questlog.game.repository;

import com.radusclifos.questlog.game.Game;
import com.radusclifos.questlog.game.dto.GamePlatform;
import org.springframework.data.jpa.repository.Query;
import com.radusclifos.questlog.game.dto.GameSuggestion;
import com.radusclifos.questlog.shared.search.SearchRepository;

import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface GameRepository extends SearchRepository<Game, Long> {

    List<GameSuggestion> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Select only the fields needed by the platform picker.
    @Query("""
            select new com.radusclifos.questlog.game.dto.GamePlatform(p.id, p.name)
            from Game g join g.platforms p where g.id = :gameId
            order by lower(p.name), p.id
            """)
    List<GamePlatform> findPlatformsByGameId(Long gameId);

    Optional<Game> findByIgdbId(Long igdbId);

    List<Game> findAllByIgdbIdIn(Collection<Long> igdbIds);

}

package com.radusclifos.questlog.game.repository;

import com.radusclifos.questlog.game.GameType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameTypeRepository extends JpaRepository<GameType, Long> {

    Optional<GameType> findByIgdbId(Long igdbId);

}

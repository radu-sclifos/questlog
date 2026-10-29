package com.radusclifos.questlog.game.repository;

import com.radusclifos.questlog.game.Engine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EngineRepository extends JpaRepository<Engine, Long> {

    Optional<Engine> findByIgdbId(Long igdbId);

}

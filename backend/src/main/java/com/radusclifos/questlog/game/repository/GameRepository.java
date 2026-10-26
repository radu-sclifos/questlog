package com.radusclifos.questlog.game.repository;

import com.radusclifos.questlog.game.Game;
import org.springframework.data.jpa.repository.JpaRepository;


public interface GameRepository extends JpaRepository<Game, Long> {
}

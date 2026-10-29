package com.radusclifos.questlog.game.repository;

import com.radusclifos.questlog.game.Game;
import com.radusclifos.questlog.game.GameRelation;
import com.radusclifos.questlog.game.GameRelationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRelationRepository extends JpaRepository<GameRelation, Long> {

    boolean existsBySourceGameAndTargetGameAndRelationType(Game source, Game target, GameRelationType type);

    @Modifying(flushAutomatically = true)
    @Query("delete from GameRelation r where r.sourceGame = :game")
    void deleteForSource(@Param("game") Game game);

}

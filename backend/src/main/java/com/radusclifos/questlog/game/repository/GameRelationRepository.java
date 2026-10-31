package com.radusclifos.questlog.game.repository;

import com.radusclifos.questlog.game.Game;
import com.radusclifos.questlog.game.GameRelation;
import com.radusclifos.questlog.game.GameRelationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface GameRelationRepository extends JpaRepository<GameRelation, Long> {

    boolean existsBySourceGameAndTargetGameAndRelationType(Game source, Game target, GameRelationType type);

    @Modifying
    @Query("delete from GameRelation r where r.sourceGame.id in :gameIds")
    void deleteForSources(@Param("gameIds") Collection<Long> gameIds);

}

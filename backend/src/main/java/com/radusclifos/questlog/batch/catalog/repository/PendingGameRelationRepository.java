package com.radusclifos.questlog.batch.catalog.repository;

import com.radusclifos.questlog.batch.catalog.PendingGameRelation;
import com.radusclifos.questlog.game.Game;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import java.util.List;

public interface PendingGameRelationRepository extends JpaRepository<PendingGameRelation, Long> {
    List<PendingGameRelation> findByIdGreaterThanOrderByIdAsc(Long afterId, Pageable page);

    @Modifying(flushAutomatically = true)
    @Query("delete from PendingGameRelation r where r.source = :game")
    void deleteForSource(@org.springframework.data.repository.query.Param("game") Game game);
}

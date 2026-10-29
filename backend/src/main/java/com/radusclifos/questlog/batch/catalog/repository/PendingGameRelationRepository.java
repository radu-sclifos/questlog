package com.radusclifos.questlog.batch.catalog.repository;

import com.radusclifos.questlog.batch.catalog.PendingGameRelation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface PendingGameRelationRepository extends JpaRepository<PendingGameRelation, Long> {
    List<PendingGameRelation> findByIdGreaterThanOrderByIdAsc(Long afterId, Pageable page);

    @Modifying
    @Query("delete from PendingGameRelation r where r.source.id in :gameIds")
    void deleteForSources(@org.springframework.data.repository.query.Param("gameIds") Collection<Long> gameIds);
}

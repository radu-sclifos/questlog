package com.radusclifos.questlog.game.repository;

import com.radusclifos.questlog.game.Series;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SeriesRepository extends JpaRepository<Series, Long> {

    Optional<Series> findByIgdbId(Long igdbId);

    List<Series> findAllByIgdbIdIn(Collection<Long> igdbIds);

}

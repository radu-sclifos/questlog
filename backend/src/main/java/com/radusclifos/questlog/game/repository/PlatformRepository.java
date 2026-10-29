package com.radusclifos.questlog.game.repository;

import com.radusclifos.questlog.game.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlatformRepository extends JpaRepository<Platform, Long> {

    Optional<Platform> findByIgdbId(Long igdbId);

    List<Platform> findAllByIgdbIdIn(Collection<Long> igdbIds);

}

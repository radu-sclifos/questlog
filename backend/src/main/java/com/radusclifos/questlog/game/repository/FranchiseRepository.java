package com.radusclifos.questlog.game.repository;

import com.radusclifos.questlog.game.Franchise;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FranchiseRepository extends JpaRepository<Franchise, Long> {

    Optional<Franchise> findByIgdbId(Long igdbId);

}

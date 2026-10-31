package com.radusclifos.questlog.game.repository;

import com.radusclifos.questlog.game.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    Optional<Genre> findByIgdbId(Long igdbId);

}

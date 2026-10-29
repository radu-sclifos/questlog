package com.radusclifos.questlog.game.repository;

import com.radusclifos.questlog.game.GameCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface GameCompanyRepository extends JpaRepository<GameCompany, Long> {

    @Modifying
    @Query("delete from GameCompany c where c.game.id in :gameIds")
    void deleteForGames(@Param("gameIds") Collection<Long> gameIds);

}

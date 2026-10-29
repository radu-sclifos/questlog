package com.radusclifos.questlog.game.repository;

import com.radusclifos.questlog.game.Game;
import com.radusclifos.questlog.game.GameCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameCompanyRepository extends JpaRepository<GameCompany, Long> {

    @Modifying(flushAutomatically = true)
    @Query("delete from GameCompany c where c.game = :game")
    void deleteForGame(@Param("game") Game game);

}

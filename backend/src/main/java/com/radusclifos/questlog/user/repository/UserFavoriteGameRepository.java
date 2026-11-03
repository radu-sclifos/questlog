package com.radusclifos.questlog.user.repository;

import com.radusclifos.questlog.user.UserFavoriteGame;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserFavoriteGameRepository extends JpaRepository<UserFavoriteGame, Long> {

    @EntityGraph(attributePaths = "game")
    List<UserFavoriteGame> findByUserIdOrderByPosition(Long userId);

    @Modifying
    @Query("delete from UserFavoriteGame f where f.user.id = :userId")
    void deleteForUser(Long userId);

}

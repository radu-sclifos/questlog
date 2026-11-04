package com.radusclifos.questlog.user.repository;

import com.radusclifos.questlog.user.UserAvatar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserAvatarRepository extends JpaRepository<UserAvatar, Long> {

    @Query("select a.revision from UserAvatar a where a.userId = :userId")
    String findRevisionByUserId(Long userId);

}

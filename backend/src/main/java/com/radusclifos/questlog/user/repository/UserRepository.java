package com.radusclifos.questlog.user.repository;

import com.radusclifos.questlog.user.User;
import com.radusclifos.questlog.user.dto.UserSuggestion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
            select new UserSuggestion(u.username, u.displayName, a.revision)
            from User u left join UserAvatar a on a.userId = u.id
            where u.enabled = true and
              (locate(lower(:name), lower(u.username)) > 0 or locate(lower(:name), lower(u.displayName)) > 0)
            order by lower(u.displayName), lower(u.username), u.id
            """)
    List<UserSuggestion> suggestions(String name, Pageable pageable);

    Optional<User> findByUsername(String username);

}

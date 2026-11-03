package com.radusclifos.questlog.user;

import com.radusclifos.questlog.shared.search.SearchIgnore;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String username;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(length = 300)
    private String bio;

    @Column(nullable = false)
    @SearchIgnore
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public User(String username, String displayName, String passwordHash) {
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.enabled = true;
    }

    public void rename(String displayName) {
        this.displayName = displayName.strip();
    }

    public void disable() {
        this.enabled = false;
    }

    public void describe(String bio) {
        if (StringUtils.isBlank(bio)) {
            this.bio = null;
        } else {
            this.bio = bio.strip();
        }
    }

}

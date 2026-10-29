package com.radusclifos.questlog.game;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "games")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) //Requested for JPA (Jakarta Persistence)
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Setter
    @Column(name = "igdb_id", unique = true)
    private Long igdbId;

    @Setter
    @Column(name = "first_release_date")
    private LocalDate firstReleaseDate;

    @Setter
    @Column(columnDefinition = "text")
    private String summary;

    @Setter
    @Column(columnDefinition = "text")
    private String storyline;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_type_id")
    private GameType gameType;

    @Setter
    @Column(name = "cover_image_id")
    private String coverImageId;

    @Setter
    @Column(name = "version_title", columnDefinition = "text")
    private String versionTitle;

    @ManyToMany
    @JoinTable(name = "game_platforms", joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "platform_id"))
    private Set<Platform> platforms = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "game_genres", joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "game_engines", joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "engine_id"))
    private Set<Engine> engines = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "game_franchises", joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "franchise_id"))
    private Set<Franchise> franchises = new HashSet<>();

    @OneToMany(mappedBy = "game")
    private Set<GameCompany> involvedCompanies = new HashSet<>();

    @OneToMany(mappedBy = "sourceGame")
    private Set<GameRelation> relations = new HashSet<>();

    @Setter
    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

    @Setter
    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Game(String title, String coverImageId) {
        rename(title);
        this.coverImageId = coverImageId;
    }

    public void rename(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("The title is mandatory");
        }

        if (title.strip().length() > 200) {
            throw new IllegalArgumentException("The title can't be longer than 200 characters");
        }

        this.name = title.strip();
    }

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }

}

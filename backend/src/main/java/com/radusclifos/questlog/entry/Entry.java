package com.radusclifos.questlog.entry;

import com.radusclifos.questlog.user.User;
import com.radusclifos.questlog.game.Game;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "entries", uniqueConstraints = @UniqueConstraint(
        name = "entries_user_game_unique", columnNames = {"user_id", "game_id"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Entry {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column
    private Double score;

    @Column(length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private EntryStatus status = EntryStatus.UNPLAYED;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EntryOwnership ownership;

    @Column
    private Integer playtime;

    public Entry(Game game) {
        setGame(game);
    }

    public void setGame(Game game) {
        this.game = java.util.Objects.requireNonNull(game, "A game is required");
    }

    public void setStatus(EntryStatus status) {
        this.status = status == null ? EntryStatus.UNPLAYED : status;
    }

}

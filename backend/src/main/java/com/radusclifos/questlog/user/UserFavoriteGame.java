package com.radusclifos.questlog.user;

import com.radusclifos.questlog.game.Game;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_favorite_games")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFavoriteGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Game game;

    @Column(nullable = false)
    private int position;

    public UserFavoriteGame(User user, Game game, int position) {
        this.user = user;
        this.game = game;
        this.position = position;
    }

}

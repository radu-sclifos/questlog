package com.radusclifos.questlog.game;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "game_relations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source_game_id", "target_game_id", "relation_type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_game_id", nullable = false)
    private Game sourceGame;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_game_id", nullable = false)
    private Game targetGame;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 40)
    private GameRelationType relationType;

    public GameRelation(Game sourceGame, Game targetGame, GameRelationType relationType) {
        this.sourceGame = java.util.Objects.requireNonNull(sourceGame);
        this.targetGame = java.util.Objects.requireNonNull(targetGame);
        this.relationType = java.util.Objects.requireNonNull(relationType);
        if (sourceGame == targetGame || (sourceGame.getId() != null
                && sourceGame.getId().equals(targetGame.getId()))) {
            throw new IllegalArgumentException("A game cannot reference itself");
        }
    }

}

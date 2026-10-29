package com.radusclifos.questlog.batch.catalog;

import com.radusclifos.questlog.game.Game;
import com.radusclifos.questlog.game.GameRelationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pending_game_relations", uniqueConstraints = @UniqueConstraint(columnNames = {"source_game_id", "target_igdb_id", "relation_type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PendingGameRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_game_id", nullable = false)
    private Game source;

    @Column(name = "target_igdb_id", nullable = false)
    private Long targetIgdbId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 40)
    private GameRelationType type;

    public PendingGameRelation(Game source, Long targetIgdbId, GameRelationType type) {
        this.source = source;
        this.targetIgdbId = targetIgdbId;
        this.type = type;
    }

}

package com.radusclifos.questlog.game;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "game_companies", uniqueConstraints =
@UniqueConstraint(columnNames = {"game_id", "company_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Setter
    @Column(nullable = false)
    private boolean developer;

    @Setter
    @Column(nullable = false)
    private boolean publisher;

    @Setter
    @Column(nullable = false)
    private boolean porting;

    @Setter
    @Column(nullable = false)
    private boolean supporting;

    public GameCompany(Game game, Company company) {
        this.game = java.util.Objects.requireNonNull(game);
        this.company = java.util.Objects.requireNonNull(company);
    }

}

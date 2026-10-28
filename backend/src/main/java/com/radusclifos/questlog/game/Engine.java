package com.radusclifos.questlog.game;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "engines")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Engine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "igdb_id", unique = true)
    private Long igdbId;

    @Column(nullable = false, columnDefinition = "text")
    private String name;

    public Engine(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The name is mandatory");
        }
        this.name = name.strip();
    }

}

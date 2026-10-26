package com.radusclifos.questlog.game;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "games")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) //Requested for JPA (Jakarta Persistence)
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "cover_url", length = 2048)
    private String coverUrl;

    public Game(String title, String coverUrl) {
        rename(title);
        this.coverUrl = coverUrl;
    }

    public void rename(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("The title is mandatory");
        }

        if (title.strip().length() > 200) {
            throw new IllegalArgumentException("The title can't be longer than 200 characters");
        }

        this.title = title;
    }
}
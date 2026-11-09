package com.radusclifos.questlog.game;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@BatchSize(size = 100)
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "igdb_id", unique = true)
    private Long igdbId;

    @Column(nullable = false, columnDefinition = "text")
    private String name;

    private Integer countryCode;

    private LocalDate foundedDate;

    private Long foundedDateFormat;

    @Column(columnDefinition = "text")
    private String logoImageId;

    public Company(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The name is mandatory");
        }
        this.name = name.strip();
    }

}

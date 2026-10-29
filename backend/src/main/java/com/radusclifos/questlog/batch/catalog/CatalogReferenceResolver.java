package com.radusclifos.questlog.batch.catalog;

import com.radusclifos.questlog.game.*;
import com.radusclifos.questlog.game.repository.*;
import com.radusclifos.questlog.integration.igdb.IgdbReference;
import lombok.RequiredArgsConstructor;

/**
 * Finds shared catalog records, such as platforms and companies, using the IGDB ID from a IgdbReference.
 * Creates missing records, updates existing names from IGDB, and returns the local entity to associate with a game,
 * preserving its internal ID.
 */
@RequiredArgsConstructor
public class CatalogReferenceResolver {

    private final PlatformRepository platforms;
    private final GenreRepository genres;
    private final EngineRepository engines;
    private final FranchiseRepository franchises;
    private final CompanyRepository companies;
    private final GameTypeRepository gameTypes;

    public Platform platform(IgdbReference source) {
        if (source == null) return null;
        validate(source, "Platform");

        var platform = platforms.findByIgdbId(source.id()).orElseGet(() -> {
            // Add if not found
            var created = new Platform(source.name());
            created.setIgdbId(source.id());
            return platforms.save(created);
        });
        // Update name
        platform.setName(source.name().strip());
        return platform;
    }

    public Genre genre(IgdbReference source) {
        if (source == null) return null;
        validate(source, "Genre");

        var genre = genres.findByIgdbId(source.id()).orElseGet(() -> {
            var created = new Genre(source.name());
            created.setIgdbId(source.id());
            return genres.save(created);
        });
        genre.setName(source.name().strip());
        return genre;
    }

    public Engine engine(IgdbReference source) {
        if (source == null) return null;
        validate(source, "Engine");

        var engine = engines.findByIgdbId(source.id()).orElseGet(() -> {
            var created = new Engine(source.name());
            created.setIgdbId(source.id());
            return engines.save(created);
        });
        engine.setName(source.name().strip());
        return engine;
    }

    public Franchise franchise(IgdbReference source) {
        if (source == null) return null;
        validate(source, "Franchise");

        var franchise = franchises.findByIgdbId(source.id()).orElseGet(() -> {
            var created = new Franchise(source.name());
            created.setIgdbId(source.id());
            return franchises.save(created);
        });
        franchise.setName(source.name().strip());
        return franchise;
    }

    public Company company(IgdbReference source) {
        if (source == null) return null;
        validate(source, "Company");

        var company = companies.findByIgdbId(source.id()).orElseGet(() -> {
            var created = new Company(source.name());
            created.setIgdbId(source.id());
            return companies.save(created);
        });
        company.setName(source.name().strip());
        return company;
    }

    public GameType gameType(IgdbReference source) {
        if (source == null) return null;
        validate(source, "GameType");

        var gameType = gameTypes.findByIgdbId(source.id()).orElseGet(() -> {
            var created = new GameType(source.name());
            created.setIgdbId(source.id());
            return gameTypes.save(created);
        });
        gameType.setName(source.name().strip());
        return gameType;
    }

    private void validate(IgdbReference source, String type) {
        if (source.id() == null || source.id() < 0 || source.name() == null || source.name().isBlank()) {
            throw new IllegalArgumentException("Missing expanded IGDB " + type + " metadata");
        }
    }

}


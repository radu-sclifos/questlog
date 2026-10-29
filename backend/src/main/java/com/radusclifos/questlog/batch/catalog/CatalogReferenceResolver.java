package com.radusclifos.questlog.batch.catalog;

import com.radusclifos.questlog.game.*;
import com.radusclifos.questlog.game.repository.*;
import com.radusclifos.questlog.integration.igdb.IgdbReference;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds shared catalog records, such as platforms and companies, using the IGDB ID from a IgdbReference.
 * Genres and game types are synchronized before the first game page and resolved from memory.
 * Other references are created or updated as games arrive, preserving their internal IDs.
 */
@RequiredArgsConstructor
public class CatalogReferenceResolver {

    private final PlatformRepository platforms;
    private final GenreRepository genres;
    private final EngineRepository engines;
    private final FranchiseRepository franchises;
    private final CompanyRepository companies;
    private final GameTypeRepository gameTypes;

    private final PlatformTransactionManager transactionManager;

    private Map<Long, Genre> genreCache = Map.of();
    private Map<Long, GameType> gameTypeCache = Map.of();

    /**
     * Synchronize the small dictionaries once per import and then publish only committed records.
     */
    public void refresh(List<IgdbReference> sourceGenres, List<IgdbReference> sourceGameTypes) {
        var loadedGenres = new HashMap<Long, Genre>();
        var loadedGameTypes = new HashMap<Long, GameType>();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var existingGenres = new HashMap<Long, Genre>();
            genres.findAll().forEach(genre -> existingGenres.put(genre.getIgdbId(), genre));
            for (var source : sourceGenres) {
                validate(source, "Genre");
                var genre = existingGenres.get(source.id());
                if (genre == null) {
                    genre = new Genre(source.name());
                    genre.setIgdbId(source.id());
                }
                genre.setName(source.name().strip());
                loadedGenres.put(source.id(), genres.save(genre));
            }

            var existingGameTypes = new HashMap<Long, GameType>();
            gameTypes.findAll().forEach(type -> existingGameTypes.put(type.getIgdbId(), type));
            for (var source : sourceGameTypes) {
                validate(source, "GameType");
                var type = existingGameTypes.get(source.id());
                if (type == null) {
                    type = new GameType(source.name());
                    type.setIgdbId(source.id());
                }
                type.setName(source.name().strip());
                loadedGameTypes.put(source.id(), gameTypes.save(type));
            }
        });

        // These entities are used only for associations by ID and never modified during page writes
        genreCache = Map.copyOf(loadedGenres);
        gameTypeCache = Map.copyOf(loadedGameTypes);
    }

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

    public Genre genre(IgdbReference source) {
        return source == null || source.id() == null ? null : genreCache.get(source.id());
    }

    public GameType gameType(IgdbReference source) {
        return source == null || source.id() == null ? null : gameTypeCache.get(source.id());
    }

    private void validate(IgdbReference source, String type) {
        if (source.id() == null || source.id() < 0 || source.name() == null || source.name().isBlank()) {
            throw new IllegalArgumentException("Missing expanded IGDB " + type + " metadata");
        }
    }

}


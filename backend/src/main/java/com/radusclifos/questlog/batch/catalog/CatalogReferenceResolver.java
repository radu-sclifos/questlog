package com.radusclifos.questlog.batch.catalog;

import com.radusclifos.questlog.game.*;
import com.radusclifos.questlog.game.repository.*;
import com.radusclifos.questlog.integration.igdb.IgdbGame;
import com.radusclifos.questlog.integration.igdb.IgdbReference;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final SeriesRepository series;
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

    public PageReferences preload(List<IgdbGame> page) {
        Set<Long> platformIds = new HashSet<>();
        Set<Long> engineIds = new HashSet<>();
        Set<Long> franchiseIds = new HashSet<>();
        Set<Long> seriesIds = new HashSet<>();
        Set<Long> companyIds = new HashSet<>();
        for (var game : page) {
            collectIds(game.platforms(), platformIds);
            collectIds(game.engines(), engineIds);
            collectIds(game.franchises(), franchiseIds);
            collectIds(game.collections(), seriesIds);
            if (game.involvedCompanies() != null) {
                for (var involvement : game.involvedCompanies()) {
                    if (involvement.company() != null) companyIds.add(involvement.company().id());
                }
            }
        }
        return new PageReferences(
                platformIds.isEmpty() ? new HashMap<>() : index(platforms.findAllByIgdbIdIn(platformIds), Platform::getIgdbId),
                engineIds.isEmpty() ? new HashMap<>() : index(engines.findAllByIgdbIdIn(engineIds), Engine::getIgdbId),
                franchiseIds.isEmpty() ? new HashMap<>() : index(franchises.findAllByIgdbIdIn(franchiseIds), Franchise::getIgdbId),
                seriesIds.isEmpty() ? new HashMap<>() : index(series.findAllByIgdbIdIn(seriesIds), Series::getIgdbId),
                companyIds.isEmpty() ? new HashMap<>() : index(companies.findAllByIgdbIdIn(companyIds), Company::getIgdbId));
    }

    private static void collectIds(List<IgdbReference> sources, Set<Long> ids) {
        if (sources != null) {
            for (var source : sources) {
                if (source != null && source.id() != null) {
                    ids.add(source.id());
                }
            }
        }
    }

    private static <T> Map<Long, T> index(List<T> entities, Function<T, Long> id) {
        return entities.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    public Platform platform(IgdbReference source, PageReferences page) {
        if (source == null) return null;
        validate(source, "Platform");

        var platform = page.platforms().computeIfAbsent(source.id(), ignored -> {
            // Add if not found
            var created = new Platform(source.name());
            created.setIgdbId(source.id());
            return platforms.save(created);
        });
        // Update name
        platform.setName(source.name().strip());
        return platform;
    }

    public Engine engine(IgdbReference source, PageReferences page) {
        if (source == null) return null;
        validate(source, "Engine");

        var engine = page.engines().computeIfAbsent(source.id(), ignored -> {
            var created = new Engine(source.name());
            created.setIgdbId(source.id());
            return engines.save(created);
        });
        engine.setName(source.name().strip());
        return engine;
    }

    public Franchise franchise(IgdbReference source, PageReferences page) {
        if (source == null) return null;
        validate(source, "Franchise");

        var franchise = page.franchises().computeIfAbsent(source.id(), ignored -> {
            var created = new Franchise(source.name());
            created.setIgdbId(source.id());
            return franchises.save(created);
        });
        franchise.setName(source.name().strip());
        return franchise;
    }

    public Series series(IgdbReference source, PageReferences page) {
        if (source == null) return null;
        validate(source, "Series");

        var series = page.series().computeIfAbsent(source.id(), ignored -> {
            var created = new Series(source.name());
            created.setIgdbId(source.id());
            return this.series.save(created);
        });
        series.setName(source.name().strip());
        return series;
    }

    public Company company(IgdbReference source, PageReferences page) {
        if (source == null) return null;
        validate(source, "Company");

        var company = page.companies().computeIfAbsent(source.id(), ignored -> {
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


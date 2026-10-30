package com.radusclifos.questlog.batch.catalog;

import com.radusclifos.questlog.batch.catalog.repository.PendingGameRelationRepository;
import com.radusclifos.questlog.game.Game;
import com.radusclifos.questlog.game.GameCompany;
import com.radusclifos.questlog.game.GameRelation;
import com.radusclifos.questlog.game.GameRelationType;
import com.radusclifos.questlog.game.repository.GameCompanyRepository;
import com.radusclifos.questlog.game.repository.GameRelationRepository;
import com.radusclifos.questlog.game.repository.GameRepository;
import com.radusclifos.questlog.integration.igdb.IgdbGame;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
public class CatalogImportWriter {

    private final EntityManager em;
    private final TransactionTemplate transactionTemplate;

    private final GameRepository games;
    private final GameCompanyRepository gameCompanies;
    private final GameRelationRepository relations;
    private final PendingGameRelationRepository pendingRelations;
    private final CatalogReferenceResolver references;

    @Autowired
    public CatalogImportWriter(EntityManager em, PlatformTransactionManager manager,
                               GameRepository games, GameCompanyRepository gameCompanies,
                               GameRelationRepository relations, PendingGameRelationRepository pendingRelations,
                               CatalogReferenceResolver references) {
        this.em = em;
        this.transactionTemplate = new TransactionTemplate(manager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.games = games;
        this.gameCompanies = gameCompanies;
        this.relations = relations;
        this.pendingRelations = pendingRelations;
        this.references = references;
    }

    public record PageResult(int saved, int filtered, int skipped) {
    }

    /**
     * Save the page together when possible; retry games individually if a record has bad data.
     */
    public PageResult writePageSkippingInvalid(List<IgdbGame> page) {
        // Keep games excluded by our rules separate from games skipped because of errors
        int filtered = (int) page.stream().filter(game -> game.exclusionReason() != null).count();

        try {
            // One transaction per page keeps normal imports fast
            return new PageResult(writePage(page), filtered, 0);
        } catch (RuntimeException pageFailure) {
            if (!isRecordError(pageFailure)) { // Database outages and unexpected errors still stop the import !!!
                throw pageFailure;
            }
            log.warn("Catalog page rolled back due to a data error; retrying games individually.");
        }

        // If we reach this point, the page has been rolled back
        // Each game now gets its own transaction
        int saved = 0;
        int skipped = 0;
        for (var game : page) {
            if (game.exclusionReason() != null) continue;
            try {
                saved += writePage(List.of(game));
            } catch (RuntimeException failure) { // Caught the problematic record
                if (!isRecordError(failure)) { // Database outages and unexpected errors still stop the import !!!
                    throw failure;
                }
                // This game's changes were rolled back so any previous data is still there
                skipped++;
                log.error("Skipping catalog game igdbId={}, name={}: {}: {}",
                        game.id(), game.name(), failure.getClass().getSimpleName(), failure.getMessage());
            }
        }
        return new PageResult(saved, filtered, skipped);
    }

    // Check if the error is related to the record
    private static boolean isRecordError(RuntimeException failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof IllegalArgumentException || cause instanceof DateTimeException
                    || cause instanceof DataIntegrityViolationException
                    || cause instanceof DataException
                    || cause instanceof ConstraintViolationException) {
                return true;
            }
        }
        return false;
    }

    // Strict atomic write, also used for each isolated retry. Returns the number of saved games.
    public int writePage(List<IgdbGame> games) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            int saved = 0;

            // Filtering the games for import
            var eligible = games.stream().filter(game -> game.exclusionReason() == null).toList();

            // Preloading properties during a single transaction for performance’s sake
            var pageReferences = references.preload(eligible);

            // Include existing relation targets too, newly inserted games enter this map as they get created
            var ids = new HashSet<Long>();
            for (var game : eligible) {
                ids.add(game.id());
                game.links().forEach(link -> ids.add(link.targetIgdbId()));
            }
            Map<Long, Game> pageGames = new HashMap<>();
            if (!ids.isEmpty()) {
                this.games.findAllByIgdbIdIn(ids).forEach(game -> pageGames.put(game.getIgdbId(), game));
            }

            // Delete existing outgoing links in bulk before rebuilding them from the imported data
            var existingSourceIds = eligible.stream().map(game -> pageGames.get(game.id()))
                    .filter(Objects::nonNull).map(Game::getId).distinct().toList();
            if (!existingSourceIds.isEmpty()) {
                gameCompanies.deleteForGames(existingSourceIds);
                relations.deleteForSources(existingSourceIds);
                pendingRelations.deleteForSources(existingSourceIds);
            }

            for (var game : eligible) {
                saveGame(game, pageGames, pageReferences);
                saved++;
            }

            em.flush();
            em.clear();
            return saved;
        }));
    }

    private void saveGame(IgdbGame sourceGame, Map<Long, Game> pageGames, PageReferences pageReferences) {
        if (sourceGame.id() == null || sourceGame.id() <= 0
                || sourceGame.name() == null || sourceGame.name().isBlank()) {
            throw new IllegalArgumentException("Invalid IGDB game identity/name: " + sourceGame.id());
        }

        // Look for an already existing Game
        var game = pageGames.get(sourceGame.id());

        // Game gets created if it couldn't be found
        if (game == null) {
            game = new Game(sourceGame.name(), null);
            game.setIgdbId(sourceGame.id());
            games.save(game);
            pageGames.put(sourceGame.id(), game);
        } else {
            game.rename(sourceGame.name());
        }

        // Set Game properties
        game.setFirstReleaseDate(sourceGame.firstReleaseDate() == null ? null
                : Instant.ofEpochSecond(sourceGame.firstReleaseDate()).atZone(ZoneOffset.UTC).toLocalDate());
        game.setSummary(sourceGame.summary());
        game.setStoryline(sourceGame.storyline());
        game.setCoverImageId(sourceGame.cover() == null ? null : sourceGame.cover().imageId());
        game.setVersionTitle(sourceGame.versionTitle());
        game.setSourceUpdatedAt(sourceGame.updatedAt() == null ? null : Instant.ofEpochSecond(sourceGame.updatedAt()));
        game.setLastSyncedAt(Instant.now());
        // Resolve shared metadata from IGDB references
        game.setGameType(references.gameType(sourceGame.gameType()));
        game.getPlatforms().clear();
        for (var item : safe(sourceGame.platforms())) {
            game.getPlatforms().add(references.platform(item, pageReferences));
        }
        game.getGenres().clear();
        for (var item : safe(sourceGame.genres())) {
            var genre = references.genre(item);
            if (genre != null) game.getGenres().add(genre);
        }
        game.getEngines().clear();
        for (var item : safe(sourceGame.engines())) {
            game.getEngines().add(references.engine(item, pageReferences));
        }
        game.getFranchises().clear();
        for (var item : safe(sourceGame.franchises())) {
            game.getFranchises().add(references.franchise(item, pageReferences));
        }

        // Collections are intentionally not loaded: they get replace/overwritten
        // This allows to keep collections always updated and not forget any old entry
        Map<Long, GameCompany> involvements = new LinkedHashMap<>();
        for (var item : safe(sourceGame.involvedCompanies())) {
            if (item.company() == null) {
                throw new IllegalArgumentException("Missing company in IGDB game " + sourceGame.id());
            }
            var company = references.company(item.company(), pageReferences);
            var involvement = involvements.get(item.company().id());
            if (involvement == null) {
                involvement = new GameCompany(game, company);
                involvements.put(item.company().id(), involvement);
            }
            involvement.setDeveloper(involvement.isDeveloper() || item.developer());
            involvement.setPublisher(involvement.isPublisher() || item.publisher());
            involvement.setPorting(involvement.isPorting() || item.porting());
            involvement.setSupporting(involvement.isSupporting() || item.supporting());
        }
        gameCompanies.saveAll(involvements.values());
        for (var link : sourceGame.links()) {
            var target = pageGames.get(link.targetIgdbId());
            if (target == null) {
                pendingRelations.save(new PendingGameRelation(game, link.targetIgdbId(), link.gameRelationType()));
            } else {
                ensureRelation(game, target, link.gameRelationType());
            }
        }
    }

    /**
     * Resolves and removes pending links, including those left by previous imports.
     */
    public long resolvePage(long afterId, int pageSize) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            var links = pendingRelations.findByIdGreaterThanOrderByIdAsc(afterId, PageRequest.of(0, pageSize));
            long cursor = afterId;
            for (var link : links) {
                cursor = link.getId();
                var target = games.findByIgdbId(link.getTargetIgdbId()).orElse(null);
                if (target == null) {
                    continue;
                }
                ensureRelation(link.getSource(), target, link.getType());
                pendingRelations.delete(link);
            }
            em.flush();
            em.clear();
            return cursor;
        }));
    }

    private void ensureRelation(Game source, Game target, GameRelationType type) {
        if (source.getId().equals(target.getId())) return;
        if (!relations.existsBySourceGameAndTargetGameAndRelationType(source, target, type)) {
            relations.save(new GameRelation(source, target, type));
        }
    }

    private static <T> List<T> safe(List<T> items) {
        return items == null ? List.of() : items;
    }

}

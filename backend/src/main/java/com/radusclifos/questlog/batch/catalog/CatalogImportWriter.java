package com.radusclifos.questlog.batch.catalog;

import com.radusclifos.questlog.game.*;
import com.radusclifos.questlog.game.repository.GameRepository;
import com.radusclifos.questlog.game.repository.GameCompanyRepository;
import com.radusclifos.questlog.game.repository.GameRelationRepository;
import com.radusclifos.questlog.batch.catalog.repository.PendingGameRelationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import com.radusclifos.questlog.integration.igdb.IgdbGame;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        this.games = games;
        this.gameCompanies = gameCompanies;
        this.relations = relations;
        this.pendingRelations = pendingRelations;
        this.references = references;
    }

    public int writePage(List<IgdbGame> games) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            int saved = 0;

            for (var game : games) {
                var exclusion = game.exclusionReason();
                if (exclusion != null) {
                    log.warn("Skipping IGDB {} {}: {}.", exclusion, game.id(), game.name());
                    continue;
                }

                saveGame(game);
                saved++;
            }

            em.flush();
            em.clear();
            return saved;
        }));
    }

    private void saveGame(IgdbGame sourceGame) {
        if (sourceGame.id() == null || sourceGame.id() <= 0
                || sourceGame.name() == null || sourceGame.name().isBlank() || sourceGame.name().strip().length() > 200) {
            throw new IllegalArgumentException("Invalid IGDB game identity/name: " + sourceGame.id());
        }

        // Look for an already existing Game
        var game = games.findByIgdbId(sourceGame.id()).orElse(null);

        // Game gets created if it couldn't be found
        if (game == null) {
            game = new Game(sourceGame.name(), null);
            game.setIgdbId(sourceGame.id());
            games.save(game);
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
            game.getPlatforms().add(references.platform(item));
        }
        game.getGenres().clear();
        for (var item : safe(sourceGame.genres())) {
            game.getGenres().add(references.genre(item));
        }
        game.getEngines().clear();
        for (var item : safe(sourceGame.engines())) {
            game.getEngines().add(references.engine(item));
        }
        game.getFranchises().clear();
        for (var item : safe(sourceGame.franchises())) {
            game.getFranchises().add(references.franchise(item));
        }

        // Collections are intentionally not loaded: they get replace/overwritten
        // This allows to keep collections always updated and not forget any old entry
        gameCompanies.deleteForGame(game);
        Map<Long, GameCompany> involvements = new LinkedHashMap<>();
        for (var item : safe(sourceGame.involvedCompanies())) {
            if (item.company() == null){
                throw new IllegalArgumentException("Missing company in IGDB game " + sourceGame.id());
            }
            var company = references.company(item.company());
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
        relations.deleteForSource(game);
        pendingRelations.deleteForSource(game);
        for (var link : sourceGame.links()) {
            var target = games.findByIgdbId(link.targetIgdbId()).orElse(null);
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
                if (target == null) continue;
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

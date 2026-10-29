package com.radusclifos.questlog.game;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class GameCatalogIntegrationTests {
    @Autowired EntityManager em;

    @Test
    void persistsReleaseDateMetadataAndDirectedCatalogLinks() {
        var platform = new Platform("PC");
        var genre = new Genre("Adventure");
        var company = new Company("Example Studio");
        var engine = new Engine("Example Engine");
        var franchise = new Franchise("Example Franchise");
        var type = new GameType("Main game");
        for (var reference : new Object[]{platform, genre, company, engine, franchise, type}) {
            em.persist(reference);
        }

        var game = new Game("Catalog example", null);
        game.setIgdbId(123456789L);
        game.setFirstReleaseDate(LocalDate.of(2024, 2, 29));
        game.setSummary("General description");
        game.setStoryline("Story premise");
        game.setCoverImageId("co_example");
        game.setGameType(type);
        game.setSourceUpdatedAt(Instant.parse("2026-10-28T01:00:00Z"));
        game.setLastSyncedAt(Instant.parse("2026-10-28T03:00:00Z"));
        game.getPlatforms().add(platform);
        game.getGenres().add(genre);
        game.getEngines().add(engine);
        game.getFranchises().add(franchise);
        em.persist(game);

        var remake = new Game("Catalog example remake", null);
        em.persist(remake);
        var involvement = new GameCompany(game, company);
        involvement.setDeveloper(true);
        involvement.setPublisher(true);
        em.persist(involvement);
        em.persist(new GameRelation(game, remake, GameRelationType.REMAKE));
        em.flush();
        var id = game.getId();
        em.clear();

        var loaded = em.find(Game.class, id);
        assertEquals(LocalDate.of(2024, 2, 29), loaded.getFirstReleaseDate());
        assertEquals("Catalog example", loaded.getName());
        assertEquals("General description", loaded.getSummary());
        assertEquals("Story premise", loaded.getStoryline());
        assertEquals("co_example", loaded.getCoverImageId());
        assertEquals("Main game", loaded.getGameType().getName());
        assertEquals(1, loaded.getPlatforms().size());
        assertEquals(1, loaded.getGenres().size());
        assertEquals(1, loaded.getEngines().size());
        assertEquals(1, loaded.getFranchises().size());
        assertEquals("Example Franchise", loaded.getFranchises().iterator().next().getName());
        var linkedCompany = loaded.getInvolvedCompanies().iterator().next();
        assertTrue(linkedCompany.isDeveloper());
        assertTrue(linkedCompany.isPublisher());
        var relation = loaded.getRelations().iterator().next();
        assertEquals(GameRelationType.REMAKE, relation.getRelationType());
        assertEquals(remake.getId(), relation.getTargetGame().getId());
        assertNotNull(loaded.getCreatedAt());
        assertNotNull(loaded.getUpdatedAt());
        assertNotEquals(loaded.getSourceUpdatedAt(), loaded.getLastSyncedAt());
    }
}

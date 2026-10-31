package com.radusclifos.questlog.game;

import com.radusclifos.questlog.game.service.GameService;
import com.radusclifos.questlog.shared.search.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:game-responses;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.open-in-view=false", "questlog.catalog-import.on-startup=false"
})
class GameServiceIntegrationTests {
    @Autowired GameService service;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;

    @Test void returnsCompleteResponsesAfterTheTransactionClosesWithoutRecursiveGames() {
        var id = new TransactionTemplate(transactions).execute(status -> {
            var game = new Game("Baldur's Gate 3", "co_example");
            game.setIgdbId(119171L);
            game.setFirstReleaseDate(LocalDate.of(2023, 8, 3));
            game.setSummary("Summary");
            game.setStoryline("Story");
            var genre = new Genre("RPG");
            genre.setIgdbId(12L);
            var platform = new Platform("PC");
            var engine = new Engine("Divinity Engine");
            var franchise = new Franchise("Forgotten Realms");
            var series = new Series("Baldur's Gate");
            var company = new Company("Larian");
            var type = new GameType("Main Game");
            for (var reference : List.of(genre, platform, engine, franchise, series, company, type)) em.persist(reference);
            game.setGameType(type);
            game.getGenres().add(genre);
            game.getPlatforms().add(platform);
            game.getEngines().add(engine);
            game.getFranchises().add(franchise);
            game.getSeries().add(series);
            em.persist(game);
            var involvement = new GameCompany(game, company);
            involvement.setDeveloper(true);
            involvement.setPublisher(true);
            em.persist(involvement);
            var related = new Game("Related game", "co_related");
            em.persist(related);
            em.persist(new GameRelation(game, related, GameRelationType.REMAKE));
            em.persist(new GameRelation(related, game, GameRelationType.REMASTER));
            return game.getId();
        });

        var response = service.findById(id);
        assertEquals("Baldur's Gate 3", response.getName());
        assertEquals(119171L, response.getIgdbId());
        assertEquals(LocalDate.of(2023, 8, 3), response.getFirstReleaseDate());
        assertEquals("Summary", response.getSummary());
        assertEquals("Story", response.getStoryline());
        assertEquals("co_example", response.getCoverImageId());
        assertEquals("Main Game", response.getGameType().getName());
        assertEquals("PC", response.getPlatforms().getFirst().getName());
        assertEquals("RPG", response.getGenres().getFirst().getName());
        assertEquals(12L, response.getGenres().getFirst().getIgdbId());
        assertNotNull(response.getGenres().getFirst().getId());
        assertEquals("Divinity Engine", response.getEngines().getFirst().getName());
        assertEquals("Forgotten Realms", response.getFranchises().getFirst().getName());
        assertEquals("Baldur's Gate", response.getSeries().getFirst().getName());
        var involvement = response.getInvolvedCompanies().getFirst();
        assertEquals("Larian", involvement.getCompany().getName());
        assertTrue(involvement.isDeveloper());
        assertTrue(involvement.isPublisher());
        assertFalse(involvement.isPorting());
        assertFalse(involvement.isSupporting());
        assertEquals(GameRelationType.REMAKE, response.getRelations().getFirst().getRelationType());
        assertEquals("Related game", response.getRelations().getFirst().getGame().getName());
        var criteria = new SearchCriteria(List.of(new FilterCriteria("id", FilterOperator.EQUAL, id)), List.of());
        assertEquals(response, service.search(criteria).getItems().getFirst());
        assertEquals(List.of(new com.radusclifos.questlog.game.dto.GameSuggestion(id, response.getName())),
                service.suggestions("bALDur", 10));
    }

}

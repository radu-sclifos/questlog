package com.radusclifos.questlog.game;

import com.radusclifos.questlog.company.Company;
import com.radusclifos.questlog.game.service.GameService;
import com.radusclifos.questlog.shared.search.FilterCriteria;
import com.radusclifos.questlog.shared.search.FilterOperator;
import com.radusclifos.questlog.shared.search.SearchCriteria;
import com.radusclifos.questlog.support.PostgresTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@PostgresTest
@SpringBootTest(properties = {
        "spring.jpa.open-in-view=false", "questlog.catalog-import.on-startup=false"
})
class GameServiceIntegrationTests {

    @Autowired
    GameService service;

    @Autowired
    EntityManager em;

    @Autowired
    PlatformTransactionManager transactions;

    @Autowired
    JsonMapper jsonMapper;

    @Test
    void returnsCompleteResponsesAfterTheTransactionClosesWithoutRecursiveGames() {
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
            company.setIgdbId(42L);
            company.setCountryCode(56);
            company.setFoundedDate(LocalDate.of(1996, 1, 1));
            company.setFoundedDateFormat(2L);
            company.setLogoImageId("larian_logo");
            var type = new GameType("Main Game");
            for (var reference : List.of(genre, platform, engine, franchise, series, company, type))
                em.persist(reference);
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
        var companyJson = jsonMapper.valueToTree(response)
                .path("involvedCompanies").path(0).path("company");
        assertEquals(3, companyJson.size());
        assertTrue(companyJson.has("id"));
        assertEquals(42L, companyJson.path("igdbId").longValue());
        assertEquals("Larian", companyJson.path("name").stringValue());
        assertEquals(GameRelationType.REMAKE, response.getRelations().getFirst().getRelationType());
        assertEquals("Related game", response.getRelations().getFirst().getGame().getName());
        var criteria = new SearchCriteria(List.of(new FilterCriteria("id", FilterOperator.EQUAL, id)), List.of());
        assertEquals(response, service.search(criteria).getItems().getFirst());
        assertEquals(List.of(new com.radusclifos.questlog.game.dto.GameSuggestion(id, response.getName(),
                        response.getCoverImageId(), response.getFirstReleaseDate())),
                service.suggestions("bALDur", 10));
    }

    @Test
    void platformsReturnsOnlyLinkedPlatformsAndDistinguishesMissingGames() {
        var id = new TransactionTemplate(transactions).execute(status -> {
            var game = new Game("Platform lookup", null);
            var pc = new Platform("PC");
            var console = new Platform("Console");
            em.persist(pc);
            em.persist(console);
            em.persist(new Platform("Unrelated"));
            game.getPlatforms().add(pc);
            game.getPlatforms().add(console);
            em.persist(game);
            return game.getId();
        });
        var platforms = service.platforms(id);
        assertEquals(List.of("Console", "PC"), platforms.stream().map(p -> p.name()).toList());
        assertNotNull(platforms.getFirst().id());
        var emptyId = new TransactionTemplate(transactions).execute(status -> {
            var game = new Game("No platforms", null);
            em.persist(game);
            return game.getId();
        });
        assertTrue(service.platforms(emptyId).isEmpty());
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.platforms(Long.MAX_VALUE));
        assertEquals(404, error.getStatusCode().value());
    }

}

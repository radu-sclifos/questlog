package com.radusclifos.questlog.entry;

import com.radusclifos.questlog.entry.dto.SaveEntryRequest;
import com.radusclifos.questlog.entry.service.EntryService;
import com.radusclifos.questlog.entry.repository.EntryRepository;
import com.radusclifos.questlog.game.Game;
import com.radusclifos.questlog.game.repository.GameRepository;
import com.radusclifos.questlog.user.User;
import com.radusclifos.questlog.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:entry-game-link;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "questlog.catalog-import.on-startup=false", "spring.jpa.open-in-view=false"
})
class EntryGameIntegrationTests {
    @Autowired EntryService service;
    @Autowired GameRepository games;
    @Autowired UserRepository users;
    @Autowired EntryRepository entries;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;

    @Test void entriesReferenceSharedCatalogDataAndDtosRemainUsableOutsideTransactions() {
        users.save(new User("entry-game-owner", "Owner", "hash"));
        var game = games.save(new Game("Original", "co_original"));
        var request = new SaveEntryRequest(game.getId(), 9.0, "Notes", null, null, null);
        var first = service.create(() -> "entry-game-owner", request);
        users.save(new User("entry-game-other", "Other", "hash"));
        var second = service.create(() -> "entry-game-other", request);
        assertNotEquals(first.getId(), second.getId());
        assertEquals(first.getGame().getId(), second.getGame().getId());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            var stored = games.findById(game.getId()).orElseThrow();
            stored.rename("Renamed");
            stored.setCoverImageId("co_updated");
        });
        var response = service.get(() -> "entry-game-owner", first.getId());
        assertEquals("Renamed", response.getGame().getName());
        assertEquals("co_updated", response.getGame().getCoverImageId());
        assertEquals(9.0, response.getScore());
        assertEquals(EntryStatus.UNPLAYED, response.getStatus());
        var columns = jdbc.queryForList("select column_name from information_schema.columns where table_name='entries' order by ordinal_position", String.class);
        assertEquals(List.of("id", "game_id", "user_id"), columns.subList(0, 3));
        assertFalse(columns.contains("title"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("insert into entries(user_id) values (?)", users.findByUsername("entry-game-owner").orElseThrow().getId()));
        service.delete(() -> "entry-game-owner", first.getId());
        assertTrue(games.existsById(game.getId()));
    }
    @Test void rejectsDuplicateCreatesAndUpdatesButAllowsSavingTheSameEntry() {
        users.save(new User("duplicate-owner", "Owner", "hash"));
        var firstGame = games.save(new Game("First", null));
        var otherGame = games.save(new Game("Other", null));
        var request = new SaveEntryRequest(firstGame.getId(), null, null, null, null, null);
        var first = service.create(() -> "duplicate-owner", request);
        var duplicate = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.create(() -> "duplicate-owner", request));
        assertEquals(409, duplicate.getStatusCode().value());
        assertEquals("This game is already in your library", duplicate.getReason());
        service.update(() -> "duplicate-owner", first.getId(), request);
        var other = service.create(() -> "duplicate-owner",
                new SaveEntryRequest(otherGame.getId(), null, null, null, null, null));
        var conflict = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.update(() -> "duplicate-owner", other.getId(), request));
        assertEquals(409, conflict.getStatusCode().value());
        assertEquals(otherGame.getId(), service.get(() -> "duplicate-owner", other.getId()).getGame().getId());
    }
}

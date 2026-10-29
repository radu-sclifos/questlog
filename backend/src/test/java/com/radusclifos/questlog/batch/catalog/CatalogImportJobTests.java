package com.radusclifos.questlog.batch.catalog;

import com.radusclifos.questlog.QuestlogApplication;
import com.radusclifos.questlog.integration.igdb.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.batch.core.BatchStatus;

@SpringBootTest(classes = QuestlogApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:catalog-job;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                "spring.datasource.username=sa", "spring.datasource.password=",
                "questlog.igdb.enabled=true", "questlog.igdb.client-id=test-client",
                "questlog.igdb.client-secret=test-secret", "questlog.igdb.verify-on-startup=false",
                "questlog.catalog-import.on-startup=false"
        })
class CatalogImportJobTests {
    @Autowired CatalogImportLauncher launcher;
    @Autowired CatalogImportWriter writer;
    @Autowired CatalogSyncHistory history;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean IgdbClient client;

    @BeforeEach
    void clearCatalog() {
        when(client.fetchCatalogGames(anyLong(), anyLong(), anyLong(), anyInt())).thenReturn(List.of());
        jdbc.update("delete from BATCH_JOB_EXECUTION_PARAMS where PARAMETER_NAME = 'syncMode'");
        for (var table : List.of("game_relations", "pending_game_relations", "game_companies",
                "game_engines", "game_platforms", "game_genres", "game_franchises", "games",
                "engines", "platforms", "genres", "franchises", "game_types", "companies")) {
            jdbc.update("delete from " + table);
        }
    }

    @Test
    void importsMetadataAndUpdatesWithoutDuplicatingSharedReferences() throws Exception {
        var original = game(101, "Original", """
                ,"first_release_date":1709164800,"summary":"Overview","storyline":"Premise",
                "updated_at":1709164800,"cover":{"image_id":"co_example"},
                "platforms":[{"id":6,"name":"PC"}],"genres":[{"id":12,"name":"RPG"}],
                "game_engines":[{"id":4,"name":"Engine"}],
                "game_type":{"id":0,"type":"Main game"},
                "franchises":[{"id":8,"name":"Franchise"}],
                "involved_companies":[{"id":10,"company":{"id":20,"name":"Studio"},"developer":true}],
                "remakes":[102]
                """);
        var remake = game(102, "Remake", """
                ,"platforms":[{"id":6,"name":"PC"}],
                "involved_companies":[{"id":11,"company":{"id":20,"name":"Studio"},"publisher":true}]
                """);
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(0L), eq(25))).thenAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                    "Provider calls must not hold a database transaction");
            return List.of(original, remake);
        });
        var first = launcher.start(25, 0);
        assertEquals(BatchStatus.COMPLETED, first.getStatus(), first.getAllFailureExceptions().toString());
        Long localId = jdbc.queryForObject("select id from games where igdb_id=101", Long.class);
        assertEquals(Date.valueOf(LocalDate.of(2024, 2, 29)),
                jdbc.queryForObject("select first_release_date from games where igdb_id=101", Date.class));
        assertEquals("Overview", jdbc.queryForObject("select summary from games where igdb_id=101", String.class));
        assertEquals(1, count("platforms"));
        assertEquals(1, count("companies"));
        assertEquals(1, count("engines"));
        assertEquals(1, count("game_engines"));
        assertEquals(2, count("game_companies"));
        assertEquals(1, count("game_relations"));

        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(0L), eq(25))).thenReturn(List.of(
                game(101, "Renamed", ",\"summary\":\"New overview\""), remake));
        assertEquals(BatchStatus.COMPLETED, launcher.start(25, 0).getStatus());
        assertEquals(2, count("games"));
        assertEquals(localId, jdbc.queryForObject("select id from games where igdb_id=101", Long.class));
        assertEquals("Renamed", jdbc.queryForObject("select name from games where igdb_id=101", String.class));
        assertEquals(0, count("game_relations"));
        assertEquals(0, count("game_engines"));
        assertEquals(1, count("game_companies"));
    }

    @Test
    void resolvesMissingTargetsOnLaterRunsWithoutReimportingTheSource() throws Exception {
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(0L), eq(25))).thenReturn(List.of(game(101, "Original", ",\"dlcs\":[102]")));
        assertEquals(BatchStatus.COMPLETED, launcher.start(25, 0).getStatus());
        assertEquals(1, count("pending_game_relations"));
        assertEquals(0, count("game_relations"));
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(0L), eq(25))).thenReturn(List.of(game(102, "DLC", "")));
        assertEquals(BatchStatus.COMPLETED, launcher.start(25, 0).getStatus());
        assertEquals("DLC", jdbc.queryForObject("select relation_type from game_relations", String.class));
        assertEquals(0, count("pending_game_relations"));
        assertEquals(BatchStatus.COMPLETED, launcher.start(25, 0).getStatus());
        assertEquals(1, count("game_relations"));
    }

    @Test
    void newAttemptReplaysCommittedPagesAndRollsBackPartialWrites() throws Exception {
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(0L), eq(1))).thenReturn(List.of(game(101, "First page", "")));
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(101L), eq(1))).thenReturn(List.of(game(102, "Second page", """
                ,"platforms":[{"id":6}]
                """)));
        var failed = launcher.start(1, 0);
        assertEquals(BatchStatus.FAILED, failed.getStatus());
        assertEquals(1, count("games"));
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(101L), eq(1))).thenReturn(List.of(game(102, "Recovered", "")));
        var retried = launcher.start(1, 0);
        assertEquals(BatchStatus.COMPLETED, retried.getStatus(), retried.getAllFailureExceptions().toString());
        assertEquals(2, count("games"));
        verify(client, times(2)).fetchCatalogGames(anyLong(), anyLong(), eq(0L), eq(1));
        verify(client, times(2)).fetchCatalogGames(anyLong(), anyLong(), eq(101L), eq(1));
    }

    @Test
    void excludesBundleProductsWithoutDiscardingOtherGames() throws Exception {
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(0L), eq(25))).thenReturn(List.of(
                game(101, "A collection", ",\"game_type\":{\"id\":77,\"type\":\" Bundle \"}"),
                game(102, "Standalone game", "")));
        var execution = launcher.start(25, 0);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(1, count("games"));
        assertEquals("Standalone game", jdbc.queryForObject("select name from games", String.class));
        assertEquals(0, count("game_types"));
        var step = execution.getStepExecutions().stream()
                .filter(item -> item.getStepName().equals("catalogImportGames")).findFirst().orElseThrow();
        assertEquals(1, step.getWriteCount());
        assertEquals(1, step.getFilterCount());
    }

    @Test
    void linksExistingTargetsImmediatelyAndReconcilesRemovedReferences() throws Exception {
        writer.writePage(List.of(game(102, "Existing target", "")));
        writer.writePage(List.of(game(101, "Source", ",\"dlcs\":[102],\"remakes\":[103]")));
        assertEquals(1, count("game_relations"));
        assertEquals(1, count("pending_game_relations"));
        assertEquals(103L, jdbc.queryForObject("select target_igdb_id from pending_game_relations", Long.class));

        writer.writePage(List.of(game(101, "Source updated", "")));
        assertEquals(0, count("game_relations"));
        assertEquals(0, count("pending_game_relations"));
    }

    @Test
    void removesResolvedPendingRowsWithoutSkippingTheNextPage() {
        writer.writePage(List.of(game(101, "Source", ",\"dlcs\":[102,103]")));
        assertEquals(2, count("pending_game_relations"));
        writer.writePage(List.of(game(102, "First target", ""), game(103, "Second target", "")));
        long firstCursor = writer.resolvePage(0, 1);
        assertEquals(1, count("pending_game_relations"));
        long secondCursor = writer.resolvePage(firstCursor, 1);
        assertTrue(secondCursor > firstCursor);
        assertEquals(0, count("pending_game_relations"));
        assertEquals(2, count("game_relations"));
        assertEquals(secondCursor, writer.resolvePage(secondCursor, 1));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            ",\"version_parent\":102",
            ",\"version_title\":\"Collector's Edition\"",
            ",\"game_type\":{\"id\":5,\"type\":\"Mod\"}",
            ",\"game_type\":{\"id\":77,\"type\":\"Standalone Mod\"}"
    })
    void filtersEditionsAndModsWithoutFilteringTheirBaseGame(String metadata) throws Exception {
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(0L), eq(25))).thenReturn(List.of(
                game(101, "Excluded product", metadata), game(102, "Base game", "")));
        var execution = launcher.start(25, 0);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(1, count("games"));
        assertEquals("Base game", jdbc.queryForObject("select name from games", String.class));
        assertEquals(0, count("game_relations"));
        assertEquals(0, count("pending_game_relations"));
    }

    @Test
    void keepsRemakesRemastersAndExpansionsAsSeparateGames() {
        writer.writePage(List.of(
                game(201, "Remake", ",\"game_type\":{\"id\":8,\"type\":\"Remake\"}"),
                game(202, "Remaster", ",\"game_type\":{\"id\":9,\"type\":\"Remaster\"}"),
                game(203, "Expansion", ",\"game_type\":{\"id\":2,\"type\":\"Expansion\"},\"parent_game\":201")));
        assertEquals(3, count("games"));
        assertEquals(1, count("game_relations"));
    }

    @Test
    void storesMultipleFranchisesWithoutASingularFranchise() {
        writer.writePage(List.of(game(301, "Crossover", """
                ,"franchises":[{"id":8,"name":"First franchise"},{"id":9,"name":"Second franchise"}]
                """)));
        assertEquals(2, count("franchises"));
        assertEquals(2, count("game_franchises"));
    }

    @Test
    void discoversTheCatalogThenUsesTheLastSuccessfulStartWithOverlap() throws Exception {
        var released = game(101, "Released", ",\"first_release_date\":1709164800");
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(0L), eq(25))).thenReturn(List.of(released));
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(101L), eq(25))).thenReturn(List.of());
        var first = launcher.start(25, 0);
        assertEquals(BatchStatus.COMPLETED, first.getStatus(), first.getAllFailureExceptions().toString());
        long started = first.getJobParameters().getLong("syncStartedAt");
        assertEquals(0L, first.getJobParameters().getLong("syncSince"));
        assertEquals(started, history.lastSuccessfulStart());
        assertEquals(1, count("games"));

        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(0L), eq(25))).thenReturn(List.of(
                game(101, "Updated", ",\"first_release_date\":1709164800"),
                game(102, "Newly discovered", ",\"first_release_date\":1709164800")));
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(102L), eq(25))).thenReturn(List.of());
        var delta = launcher.start(25, 0);
        assertEquals(BatchStatus.COMPLETED, delta.getStatus());
        assertEquals(started - 300, delta.getJobParameters().getLong("syncSince"));
        assertEquals(2, count("games"));
        assertEquals("Updated", jdbc.queryForObject("select name from games where igdb_id=101", String.class));
        Long previous = history.lastSuccessfulStart();
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(0L), eq(25)))
                .thenThrow(new IgdbIntegrationException("Unavailable"));
        assertEquals(BatchStatus.FAILED, launcher.start(25, 0).getStatus());
        assertEquals(previous, history.lastSuccessfulStart());
    }

    @Test
    void failedCatalogRunIsRetriedFromTheBeginningWithoutAdvancingHistory() throws Exception {
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(0L), eq(1))).thenReturn(List.of(
                game(101, "First", ",\"first_release_date\":1709164800")));
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(101L), eq(1)))
                .thenThrow(new IgdbIntegrationException("Unavailable"));
        var failed = launcher.start(1, 0);
        assertEquals(BatchStatus.FAILED, failed.getStatus());
        assertNull(history.lastSuccessfulStart());
        assertEquals(1, count("games"));
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(101L), eq(1))).thenReturn(List.of());
        var retried = launcher.start(1, 0);
        assertEquals(BatchStatus.COMPLETED, retried.getStatus());
        assertNotEquals(failed.getJobParameters().getString("run.id"), retried.getJobParameters().getString("run.id"));
        assertEquals(0L, retried.getJobParameters().getLong("syncSince"));
        assertEquals(retried.getJobParameters().getLong("syncStartedAt"), history.lastSuccessfulStart());
        assertEquals(1, count("games"));
        verify(client, times(2)).fetchCatalogGames(eq(0L), anyLong(), eq(0L), eq(1));
        verify(client, times(2)).fetchCatalogGames(eq(0L), anyLong(), eq(101L), eq(1));
    }

    @Test
    void stopsAfterTwentyPagesWithoutRequestingAnotherAndStillResolvesRelations() throws Exception {
        when(client.fetchCatalogGames(anyLong(), anyLong(), anyLong(), eq(1))).thenAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            long id = invocation.<Long>getArgument(2) + 1;
            return List.of(game(id, "Game " + id, id == 1 ? ",\"dlcs\":[2]" : ""));
        });
        var limited = launcher.start(1, 20);
        assertEquals(BatchStatus.COMPLETED, limited.getStatus(), limited.getAllFailureExceptions().toString());
        assertEquals(20, count("games"));
        assertEquals(1, count("game_relations"));
        assertEquals(0, count("pending_game_relations"));
        assertEquals(2, limited.getStepExecutions().size());
        verify(client, times(20)).fetchCatalogGames(anyLong(), anyLong(), anyLong(), eq(1));
        verify(client, never()).fetchCatalogGames(anyLong(), anyLong(), eq(20L), eq(1));
        assertNull(history.lastSuccessfulStart());

        when(client.fetchCatalogGames(anyLong(), anyLong(), anyLong(), eq(1))).thenReturn(List.of());
        var full = launcher.start(1, 0);
        assertEquals(BatchStatus.COMPLETED, full.getStatus());
        assertEquals(0L, full.getJobParameters().getLong("syncSince"));
        assertEquals(full.getJobParameters().getLong("syncStartedAt"), history.lastSuccessfulStart());
    }

    @Test
    void filteredOutPagesStillCountTowardsTheLimit() throws Exception {
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(0L), eq(25))).thenReturn(List.of(
                game(101, "Bundle", ",\"game_type\":{\"id\":77,\"type\":\"Bundle\"}")));
        var limited = launcher.start(25, 1);
        assertEquals(BatchStatus.COMPLETED, limited.getStatus());
        assertEquals(0, count("games"));
        verify(client, times(1)).fetchCatalogGames(anyLong(), anyLong(), anyLong(), eq(25));
        assertNull(history.lastSuccessfulStart());
    }

    @Test
    void endsBeforeTheLimitWhenProviderHasNoMorePagesAndPreservesPreviousSync() throws Exception {
        var full = launcher.start(25, 0);
        assertEquals(BatchStatus.COMPLETED, full.getStatus());
        Long previous = history.lastSuccessfulStart();
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(0L), eq(25))).thenReturn(List.of(game(101, "Game", "")));
        clearInvocations(client);
        var limited = launcher.start(25, 20);
        assertEquals(BatchStatus.COMPLETED, limited.getStatus());
        assertEquals(previous - 300, limited.getJobParameters().getLong("syncSince"));
        assertEquals(previous, history.lastSuccessfulStart());
        verify(client, times(2)).fetchCatalogGames(anyLong(), anyLong(), anyLong(), eq(25));
    }

    @Test
    void recoversInterruptedExecutionAndOnlyItsUnfinishedStepsBeforeTheNextRun() throws Exception {
        var successful = launcher.start(25, 0);
        Long previous = history.lastSuccessfulStart();
        var interrupted = launcher.start(25, 1);
        // Simulate persisted metadata left behind when the process is killed.
        jdbc.update("update BATCH_JOB_EXECUTION set STATUS='STARTED', END_TIME=null where JOB_EXECUTION_ID=?", interrupted.getId());
        jdbc.update("update BATCH_STEP_EXECUTION set STATUS='STARTED', END_TIME=null where JOB_EXECUTION_ID=? and STEP_NAME='catalogImportRelations'", interrupted.getId());
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(0L), eq(25))).thenAnswer(invocation -> {
            assertEquals("FAILED", jdbc.queryForObject("select STATUS from BATCH_JOB_EXECUTION where JOB_EXECUTION_ID=?", String.class, interrupted.getId()));
            assertEquals("FAILED", jdbc.queryForObject("select STATUS from BATCH_STEP_EXECUTION where JOB_EXECUTION_ID=? and STEP_NAME='catalogImportRelations'", String.class, interrupted.getId()));
            assertEquals("COMPLETED", jdbc.queryForObject("select STATUS from BATCH_STEP_EXECUTION where JOB_EXECUTION_ID=? and STEP_NAME='catalogImportGames'", String.class, interrupted.getId()));
            assertEquals(previous, history.lastSuccessfulStart());
            return List.of();
        });
        var next = launcher.start(25, 0);
        assertEquals(BatchStatus.COMPLETED, next.getStatus(), next.getAllFailureExceptions().toString());
        assertEquals(previous - 300, next.getJobParameters().getLong("syncSince"));
        assertNotNull(jdbc.queryForObject("select END_TIME from BATCH_JOB_EXECUTION where JOB_EXECUTION_ID=?", java.sql.Timestamp.class, interrupted.getId()));
        assertEquals("COMPLETED", jdbc.queryForObject("select STATUS from BATCH_JOB_EXECUTION where JOB_EXECUTION_ID=?", String.class, successful.getId()));
    }

    @Test
    void rejectsConcurrentLocalStartWithoutFailingTheActiveExecution() throws Exception {
        when(client.fetchCatalogGames(anyLong(), anyLong(), eq(0L), eq(25))).thenAnswer(invocation -> {
            assertThrows(IllegalStateException.class, () -> launcher.start(25, 0));
            assertEquals(1, jdbc.queryForObject("select count(*) from BATCH_JOB_EXECUTION where STATUS='STARTED'", Integer.class));
            return List.of();
        });
        var execution = launcher.start(25, 0);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    }

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    private IgdbGame game(long id, String name, String extra) {
        return JsonMapper.builder().build().readValue(
                "{\"id\":" + id + ",\"name\":\"" + name + "\""
                        + (extra.contains("first_release_date") ? "" : ",\"first_release_date\":1709164800") + extra + "}", IgdbGame.class);
    }
}

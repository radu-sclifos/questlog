package com.radusclifos.questlog.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestContextManager;

import static org.junit.jupiter.api.Assertions.*;

@PostgresTest
@SpringBootTest
class PostgresDatabaseResetTests {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void classBoundaryRemovesCommittedDataAndResetsSequencesButPreservesSchema() throws Exception {
        var history = jdbc.queryForList("select * from flyway_schema_history order by installed_rank");
        assertFalse(history.isEmpty());
        var game = jdbc.queryForObject("insert into games(name) values ('Cleanup fixture') returning id", Long.class);
        var user = jdbc.queryForObject("""
                insert into users(username, display_name, password_hash)
                values ('cleanup-owner', 'Owner', 'test-only-hash') returning id
                """, Long.class);
        jdbc.update("insert into entries(game_id, user_id) values (?, ?)", game, user);
        jdbc.update("""
                insert into batch_job_instance(job_instance_id, version, job_name, job_key)
                values (nextval('batch_job_instance_seq'), 0, 'cleanup', 'cleanup-key')
                """);
        jdbc.queryForObject("select nextval('batch_job_execution_seq')", Long.class);
        jdbc.queryForObject("select nextval('batch_step_execution_seq')", Long.class);

        // Exercise the actual before-class callback, not just the SQL helper
        // No test transaction, the fixture above is committed on real connections
        var manager = new TestContextManager(PostgresDatabaseResetTests.class);
        new PostgresResetTestExecutionListener().beforeTestClass(manager.getTestContext());

        for (var table : new String[]{"games", "users", "entries", "batch_job_instance"}) {
            assertEquals(0, jdbc.queryForObject("select count(*) from " + table, Integer.class), table);
        }
        assertEquals(history, jdbc.queryForList("select * from flyway_schema_history order by installed_rank"));
        assertEquals(1L, jdbc.queryForObject("insert into games(name) values ('After cleanup') returning id", Long.class));
        for (var sequence : new String[]{"batch_job_instance_seq", "batch_job_execution_seq", "batch_step_execution_seq"}) {
            assertEquals(1L, jdbc.queryForObject("select nextval('" + sequence + "')", Long.class), sequence);
        }
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("insert into entries(game_id, user_id) values (999999, 999999)"));
        PostgresDatabaseReset.reset();
        PostgresDatabaseReset.reset(); // An already empty schema can be cleaned again
    }

    @Test
    void closingOneContextDoesNotStopTheDatabaseUsedByAnother() {
        try (var first = new AnnotationConfigApplicationContext(PostgresTestConfiguration.class)) {
            var url = first.getBean(JdbcConnectionDetails.class).getJdbcUrl();
            try (var second = new AnnotationConfigApplicationContext(PostgresTestConfiguration.class)) {
                assertEquals(url, second.getBean(JdbcConnectionDetails.class).getJdbcUrl());
            }
            assertTrue(SharedPostgres.container().isRunning());
            assertEquals(1, jdbc.queryForObject("select 1", Integer.class));
        }
        assertTrue(SharedPostgres.container().isRunning());
    }

}

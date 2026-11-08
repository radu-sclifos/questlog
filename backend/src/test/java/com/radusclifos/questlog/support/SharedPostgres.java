package com.radusclifos.questlog.support;

import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * One lazy container per test JVM; Testcontainers/Ryuk cleans it up at JVM exit.
 */
final class SharedPostgres {

    private SharedPostgres() {
    }

    private static class Holder {
        private static final PostgreSQLContainer INSTANCE = start();

        private static PostgreSQLContainer start() {
            var container = PostgresTestConfiguration.newContainer();
            container.start();
            return container;
        }
    }

    static PostgreSQLContainer container() {
        return Holder.INSTANCE;
    }

    static Connection connection() throws SQLException {
        var postgres = container();
        // Never obtain the cleanup connection from environment variables or an
        // application datasource: only this disposable test container is eligible.
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

}

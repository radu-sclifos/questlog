package com.radusclifos.questlog.support;

import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {

    // Pin the same PostgreSQL major locally and in CI.
    // The infrastructure test intentionally owns a separate, fresh database.
    public static PostgreSQLContainer newContainer() {
        return new PostgreSQLContainer("postgres:17.7-alpine");
    }

    @Bean
    JdbcConnectionDetails postgresConnectionDetails() {
        var postgres = SharedPostgres.container();
        // Expose connection details rather than a container bean
        // Closing a Spring context must not stop PostgreSQL while other cached contexts still use it
        return new JdbcConnectionDetails() {
            @Override
            public String getJdbcUrl() {
                return postgres.getJdbcUrl();
            }

            @Override
            public String getUsername() {
                return postgres.getUsername();
            }

            @Override
            public String getPassword() {
                return postgres.getPassword();
            }
        };
    }

}

package com.radusclifos.questlog.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {

    // Pin the same PostgreSQL major locally and in CI.
    // Each context owns its database.
    public static PostgreSQLContainer newContainer() {
        return new PostgreSQLContainer("postgres:17.7-alpine");
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return newContainer();
    }

}

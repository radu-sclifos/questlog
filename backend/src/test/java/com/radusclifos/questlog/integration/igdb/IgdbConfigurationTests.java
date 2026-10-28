package com.radusclifos.questlog.integration.igdb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class IgdbConfigurationTests {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(IgdbConfiguration.class);

    @Test
    void disabledByDefaultAndRequiresNoCredentials() {
        context.run(ctx -> {
            assertThat(ctx).hasNotFailed().doesNotHaveBean(IgdbClient.class)
                    .doesNotHaveBean(TwitchAppTokenProvider.class)
                    .doesNotHaveBean("igdbAuthenticationCheck");
        });
    }

    @Test
    void enabledWithoutCredentialsFailsClearly() {
        context.withPropertyValues("questlog.igdb.enabled=true").run(ctx ->
                assertThat(ctx).hasFailed());
    }

    @Test
    void enablingCreatesClientWithoutNetworkCallsOrAutomaticVerification() {
        context.withPropertyValues("questlog.igdb.enabled=true",
                "questlog.igdb.client-id=test-id", "questlog.igdb.client-secret=test-secret")
                .run(ctx -> assertThat(ctx).hasNotFailed().hasSingleBean(IgdbClient.class)
                        .hasSingleBean(TwitchAppTokenProvider.class)
                        .doesNotHaveBean("igdbAuthenticationCheck"));
    }

    @Test
    void verificationRequiresIntegrationToBeEnabled() {
        context.withPropertyValues("questlog.igdb.verify-on-startup=true")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void configurationNeverPrintsCredentials() {
        var properties = new IgdbProperties(true, "private-id", "private-secret", false, null, null);
        assertFalse(properties.toString().contains("private-id"));
        assertFalse(properties.toString().contains("private-secret"));
    }
}

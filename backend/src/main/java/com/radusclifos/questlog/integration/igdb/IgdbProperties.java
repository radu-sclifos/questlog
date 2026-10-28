package com.radusclifos.questlog.integration.igdb;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("questlog.igdb")
public record IgdbProperties(boolean enabled, String clientId, String clientSecret,
                             boolean verifyOnStartup, Duration connectTimeout, Duration readTimeout) {

    public IgdbProperties {
        clientId = clientId == null ? "" : clientId.strip();
        clientSecret = clientSecret == null ? "" : clientSecret.strip();
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(15) : readTimeout;
        if (enabled && (clientId.isBlank() || clientSecret.isBlank())) {
            throw new IllegalArgumentException(
                    "IGDB enabled: set IGDB_CLIENT_ID and IGDB_CLIENT_SECRET");
        }
        if (verifyOnStartup && !enabled) {
            throw new IllegalArgumentException("IGDB_VERIFY_ON_STARTUP requires IGDB_ENABLED=true");
        }
        if (connectTimeout.isNegative() || connectTimeout.isZero()
                || readTimeout.isNegative() || readTimeout.isZero()) {
            throw new IllegalArgumentException("IGDB HTTP timeouts must be positive");
        }
    }

    @Override
    public String toString() {
        return "IgdbProperties[enabled=" + enabled + ", credentials=REDACTED]";
    }

}

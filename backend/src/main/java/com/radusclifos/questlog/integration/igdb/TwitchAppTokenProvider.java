package com.radusclifos.questlog.integration.igdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Instant;

/**
 * App credentials only: no user login or refresh token. Token stays in memory.
 */
public class TwitchAppTokenProvider {

    private final RestClient twitch;
    private final IgdbProperties properties;
    private final Clock clock;
    private String token;
    private Instant expiresAt = Instant.MIN;
    private Instant validatedAt = Instant.MIN;

    public TwitchAppTokenProvider(RestClient twitch, IgdbProperties properties, Clock clock) {
        this.twitch = twitch;
        this.properties = properties;
        this.clock = clock;
    }

    public synchronized String accessToken() {
        var now = clock.instant();
        if (token == null || !now.isBefore(expiresAt.minusSeconds(60))) {
            acquireToken();
        } else if (!now.isBefore(validatedAt.plusSeconds(3600))) {
            try {
                validateToken();
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().value() != 401) {
                    throw failure(exception.getStatusCode().value());
                }
                acquireToken();
            } catch (RestClientException exception) {
                throw new IgdbIntegrationException("Twitch token validation failed; check connectivity.");
            }
        }
        return token;
    }

    public synchronized void invalidate(String rejectedToken) {
        if (java.util.Objects.equals(token, rejectedToken)) {
            token = null;
            expiresAt = Instant.MIN;
            validatedAt = Instant.MIN;
        }
    }

    private void acquireToken() {
        token = null;
        var form = new LinkedMultiValueMap<String, String>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("grant_type", "client_credentials");
        var requestedAt = clock.instant();
        try {
            var response = twitch.post().uri("token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().body(TokenResponse.class);
            if (response == null || response.accessToken() == null || response.accessToken().isBlank()
                    || !"bearer".equalsIgnoreCase(response.tokenType()) || response.expiresIn() <= 60) {
                throw new IgdbIntegrationException("Twitch returned an invalid app token response.");
            }
            token = response.accessToken();
            expiresAt = requestedAt.plusSeconds(response.expiresIn());
            validateToken();
        } catch (RestClientResponseException exception) {
            token = null;
            throw failure(exception.getStatusCode().value());
        } catch (RestClientException exception) {
            token = null;
            throw new IgdbIntegrationException("Twitch authentication failed; check connectivity and response format.");
        }
    }

    private void validateToken() {
        var checkedAt = clock.instant();
        var response = twitch.get().uri("validate")
                .header(HttpHeaders.AUTHORIZATION, "OAuth " + token)
                .retrieve().body(ValidationResponse.class);
        if (response == null || !properties.clientId().equals(response.clientId())
                || response.expiresIn() <= 60) {
            token = null;
            throw new IgdbIntegrationException("Twitch token validation failed: client or expiry mismatch.");
        }
        var validatedExpiry = checkedAt.plusSeconds(response.expiresIn());
        if (validatedExpiry.isBefore(expiresAt)) {
            expiresAt = validatedExpiry;
        }
        validatedAt = checkedAt;
    }

    private IgdbIntegrationException failure(int status) {
        return new IgdbIntegrationException("Twitch authentication failed (HTTP " + status
                + "). Check application credentials and provider availability.");
    }

    private record TokenResponse(@JsonProperty("access_token") String accessToken,
                                 @JsonProperty("expires_in") long expiresIn,
                                 @JsonProperty("token_type") String tokenType) {
        @Override
        public String toString() {
            return "TokenResponse[REDACTED]";
        }
    }

    private record ValidationResponse(@JsonProperty("client_id") String clientId,
                                      @JsonProperty("expires_in") long expiresIn) {
    }

}

package com.radusclifos.questlog.integration.igdb;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class IgdbClient {

    private final RestClient igdb;
    private final IgdbProperties properties;
    private final TwitchAppTokenProvider tokens;

    public IgdbClient(RestClient igdb, IgdbProperties properties, TwitchAppTokenProvider tokens) {
        this.igdb = igdb;
        this.properties = properties;
        this.tokens = tokens;
    }

    /**
     * Read-only authentication probe; does not fetch or persist game records.
     */
    public void verifyAuthentication() {
        for (int attempt = 0; attempt < 2; attempt++) {
            var token = tokens.accessToken();
            try {
                var response = igdb.post().uri("games/count")
                        .header("Client-ID", properties.clientId())
                        .headers(headers -> headers.setBearerAuth(token))
                        .contentType(MediaType.TEXT_PLAIN)
                        .accept(MediaType.APPLICATION_JSON)
                        .body("where id = 0;")
                        .retrieve().body(CountResponse.class);
                if (response == null || response.count() == null || response.count() < 0) {
                    throw new IgdbIntegrationException("IGDB returned an invalid authentication probe response.");
                }
                return;
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().value() == 401) {
                    tokens.invalidate(token);
                    if (attempt == 0) {
                        continue;
                    }
                }
                throw new IgdbIntegrationException("IGDB authentication probe failed (HTTP "
                        + exception.getStatusCode().value() + "). Check credentials, rate limits and provider availability.");
            } catch (RestClientException exception) {
                throw new IgdbIntegrationException("IGDB authentication probe failed; check connectivity and response format.");
            }
        }
    }

    private record CountResponse(Long count) {
    }

}

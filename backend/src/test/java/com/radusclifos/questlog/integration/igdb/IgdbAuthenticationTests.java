package com.radusclifos.questlog.integration.igdb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class IgdbAuthenticationTests {
    private MockRestServiceServer twitch;
    private MockRestServiceServer igdb;
    private Clock clock;
    private TwitchAppTokenProvider tokens;
    private IgdbClient client;

    @BeforeEach
    void setUp() {
        var properties = new IgdbProperties(true, "client-id", "secret-value", false, null, null);
        var twitchBuilder = RestClient.builder().baseUrl("https://id.twitch.tv/oauth2/");
        twitch = MockRestServiceServer.bindTo(twitchBuilder).build();
        var igdbBuilder = RestClient.builder().baseUrl("https://api.igdb.com/v4/");
        igdb = MockRestServiceServer.bindTo(igdbBuilder).build();
        clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.parse("2026-10-28T03:00:00Z"));
        tokens = new TwitchAppTokenProvider(twitchBuilder.build(), properties, clock);
        client = new IgdbClient(igdbBuilder.build(), properties, tokens);
    }

    @Test
    void authenticatesWithFormBodyValidatesAndReusesToken() {
        expectToken("token-one", 7200);
        expectProbe("token-one", HttpStatus.OK);
        expectProbe("token-one", HttpStatus.OK);
        client.verifyAuthentication();
        client.verifyAuthentication();
        twitch.verify();
        igdb.verify();
    }

    @Test
    void refreshesBeforeExpirationWithoutARefreshToken() {
        expectToken("token-one", 120);
        expectToken("token-two", 7200);
        assertEquals("token-one", tokens.accessToken());
        when(clock.instant()).thenReturn(Instant.parse("2026-10-28T03:01:01Z"));
        assertEquals("token-two", tokens.accessToken());
        twitch.verify();
    }

    @Test
    void revalidatesCachedTokenOnUseAfterAnHour() {
        expectToken("token-one", 7200);
        expectValidation("token-one", "client-id", 3500);
        assertEquals("token-one", tokens.accessToken());
        when(clock.instant()).thenReturn(Instant.parse("2026-10-28T04:00:01Z"));
        assertEquals("token-one", tokens.accessToken());
        twitch.verify();
    }

    @Test
    void renewsRevokedCachedToken() {
        expectToken("token-one", 7200);
        twitch.expect(requestTo("https://id.twitch.tv/oauth2/validate"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        expectToken("token-two", 7200);
        tokens.accessToken();
        when(clock.instant()).thenReturn(Instant.parse("2026-10-28T04:00:01Z"));
        assertEquals("token-two", tokens.accessToken());
        twitch.verify();
    }

    @Test
    void retriesIgdb401OnlyOnceWithANewToken() {
        expectToken("token-one", 7200);
        expectToken("token-two", 7200);
        expectProbe("token-one", HttpStatus.UNAUTHORIZED);
        expectProbe("token-two", HttpStatus.OK);
        client.verifyAuthentication();
        twitch.verify();
        igdb.verify();
    }

    @Test
    void stopsAfterRepeated401AndDiscardsRejectedToken() {
        expectToken("token-one", 7200);
        expectToken("token-two", 7200);
        expectProbe("token-one", HttpStatus.UNAUTHORIZED);
        expectProbe("token-two", HttpStatus.UNAUTHORIZED);
        var error = assertThrows(IgdbIntegrationException.class, client::verifyAuthentication);
        assertTrue(error.getMessage().contains("401"));
        assertNull(error.getCause());
        twitch.verify();
        igdb.verify();
    }

    @Test
    void doesNotRetryRateLimitsOrLeakProviderBodies() {
        expectToken("token-one", 7200);
        igdb.expect(requestTo("https://api.igdb.com/v4/games/count"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .body("secret-value token-one sensitive-provider-body"));
        var error = assertThrows(IgdbIntegrationException.class, client::verifyAuthentication);
        assertTrue(error.getMessage().contains("429"));
        assertFalse(error.toString().contains("secret-value"));
        assertFalse(error.toString().contains("token-one"));
        assertNull(error.getCause());
        twitch.verify();
        igdb.verify();
    }

    @Test
    void hidesCredentialRejectionResponse() {
        twitch.expect(requestTo("https://id.twitch.tv/oauth2/token"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("secret-value"));
        var error = assertThrows(IgdbIntegrationException.class, tokens::accessToken);
        assertFalse(error.toString().contains("secret-value"));
        assertNull(error.getCause());
        twitch.verify();
    }

    @Test
    void rejectsInvalidTokenPayload() {
        twitch.expect(requestTo("https://id.twitch.tv/oauth2/token"))
                .andRespond(withSuccess("{\"expires_in\":7200,\"token_type\":\"bearer\"}", MediaType.APPLICATION_JSON));
        assertThrows(IgdbIntegrationException.class, tokens::accessToken);
        twitch.verify();
    }

    @Test
    void rejectsTokenBelongingToAnotherApplication() {
        expectTokenResponse("token-one", 7200);
        expectValidation("token-one", "another-client", 7200);
        assertThrows(IgdbIntegrationException.class, tokens::accessToken);
        twitch.verify();
    }

    @Test
    void concurrentCallersShareOneTokenAcquisition() throws Exception {
        expectToken("token-one", 7200);
        try (var executor = Executors.newFixedThreadPool(4)) {
            var results = executor.invokeAll(IntStream.range(0, 8)
                    .<java.util.concurrent.Callable<String>>mapToObj(i -> tokens::accessToken).toList());
            for (var result : results) {
                assertEquals("token-one", result.get());
            }
        }
        twitch.verify();
    }

    @Test
    void rejectsMalformedProbeResponse() {
        expectToken("token-one", 7200);
        igdb.expect(requestTo("https://api.igdb.com/v4/games/count"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThrows(IgdbIntegrationException.class, client::verifyAuthentication);
        igdb.verify();
    }

    private void expectToken(String token, long lifetime) {
        expectTokenResponse(token, lifetime);
        expectValidation(token, "client-id", lifetime);
    }

    private void expectTokenResponse(String token, long lifetime) {
        twitch.expect(requestTo("https://id.twitch.tv/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string("client_id=client-id&client_secret=secret-value&grant_type=client_credentials"))
                .andRespond(withSuccess("{\"access_token\":\"" + token
                        + "\",\"expires_in\":" + lifetime + ",\"token_type\":\"bearer\"}", MediaType.APPLICATION_JSON));
    }

    private void expectValidation(String token, String clientId, long lifetime) {
        twitch.expect(requestTo("https://id.twitch.tv/oauth2/validate"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "OAuth " + token))
                .andRespond(withSuccess("{\"client_id\":\"" + clientId + "\",\"expires_in\":"
                        + lifetime + "}", MediaType.APPLICATION_JSON));
    }

    private void expectProbe(String token, HttpStatus status) {
        var expectation = igdb.expect(requestTo("https://api.igdb.com/v4/games/count"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Client-ID", "client-id"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(content().string("where id = 0;"));
        expectation.andRespond(status == HttpStatus.OK
                ? withSuccess("{\"count\":0}", MediaType.APPLICATION_JSON) : withStatus(status));
    }
}

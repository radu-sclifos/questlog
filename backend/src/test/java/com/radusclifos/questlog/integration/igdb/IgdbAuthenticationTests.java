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
import java.util.List;
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
    void paginatesDictionariesIfTheyOutgrowOneResponse() {
        expectToken("token-one", 7200);
        var firstPage = IntStream.range(1, 501)
                .mapToObj(id -> "{\"id\":" + id + ",\"name\":\"Genre " + id + "\"}")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        igdb.expect(requestTo("https://api.igdb.com/v4/genres"))
                .andExpect(content().string("fields name; sort id asc; limit 500; offset 0;"))
                .andRespond(withSuccess(firstPage, MediaType.APPLICATION_JSON));
        igdb.expect(requestTo("https://api.igdb.com/v4/genres"))
                .andExpect(content().string("fields name; sort id asc; limit 500; offset 500;"))
                .andRespond(withSuccess("[{\"id\":501,\"name\":\"Last genre\"}]", MediaType.APPLICATION_JSON));
        var genres = client.fetchGenres();
        assertEquals(501, genres.size());
        assertEquals(501L, genres.getLast().id());
        igdb.verify();
    }

    @Test
    void rejectsDictionaryRecordsWithoutNames() {
        expectToken("token-one", 7200);
        igdb.expect(requestTo("https://api.igdb.com/v4/game_types"))
                .andRespond(withSuccess("[{\"id\":0}]", MediaType.APPLICATION_JSON));
        assertThrows(IgdbIntegrationException.class, client::fetchGameTypes);
        igdb.verify();
    }

    @Test
    void fetchesCompleteSmallDictionariesIncludingGameTypeZero() {
        expectToken("token-one", 7200);
        igdb.expect(requestTo("https://api.igdb.com/v4/genres"))
                .andExpect(content().string("fields name; sort id asc; limit 500; offset 0;"))
                .andRespond(withSuccess("[{\"id\":12,\"name\":\"RPG\"}]", MediaType.APPLICATION_JSON));
        igdb.expect(requestTo("https://api.igdb.com/v4/game_types"))
                .andExpect(content().string("fields type; sort id asc; limit 500; offset 0;"))
                .andRespond(withSuccess("[{\"id\":0,\"type\":\"Main game\"}]", MediaType.APPLICATION_JSON));
        assertEquals(List.of(new IgdbReference(12L, "RPG")), client.fetchGenres());
        assertEquals(List.of(new IgdbReference(0L, "Main game")), client.fetchGameTypes());
        igdb.verify();
        twitch.verify();
    }

    @Test
    void queriesCreatedUpdatedAndNewlyReleasedGamesInAFrozenWindow() {
        expectToken("token-one", 7200);
        igdb.expect(requestTo("https://api.igdb.com/v4/games"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "where id > 42 & first_release_date != null & first_release_date <= 2000")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "((created_at >= 1000 & created_at <= 2000) | (updated_at >= 1000 & updated_at <= 2000) | first_release_date >= 1000)")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("sort id asc; limit 25;")))
                .andRespond(withSuccess("[{\"id\":43,\"name\":\"Game\",\"first_release_date\":1500}]", MediaType.APPLICATION_JSON));
        assertEquals(43L, client.fetchCatalogGames(1000, 2000, 42, 25).getFirst().id());
        igdb.verify();
    }

    @Test
    void firstCatalogImportOnlyFiltersReleasedGamesAndRejectsUnorderedPages() {
        expectToken("token-one", 7200);
        igdb.expect(requestTo("https://api.igdb.com/v4/games"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("created_at >="))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("first_release_date <= 2000")))
                .andRespond(withSuccess("[{\"id\":2,\"first_release_date\":1000},{\"id\":1,\"first_release_date\":1000}]", MediaType.APPLICATION_JSON));
        assertThrows(IgdbIntegrationException.class, () -> client.fetchCatalogGames(0, 2000, 0, 25));
        igdb.verify();
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

    @Test
    void reportsStructuredBadRequestDetailsWithoutExposingCredentials() {
        expectToken("token-one", 7200);
        igdb.expect(requestTo("https://api.igdb.com/v4/games"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                [{"title":"Invalid field","cause":"Unknown field example; token-one secret-value client-id",
                                  "debug":"must-not-appear"}]
                                """));
        var error = assertThrows(IgdbIntegrationException.class,
                () -> client.fetchGames(List.of(1942L)));
        assertTrue(error.getMessage().contains("Invalid field"));
        assertTrue(error.getMessage().contains("Unknown field example"));
        for (var hidden : List.of("token-one", "secret-value", "client-id", "must-not-appear")) {
            assertFalse(error.getMessage().contains(hidden));
        }
        assertNull(error.getCause());
        igdb.verify();
    }

    private void expectToken(String token, long lifetime) {
        expectTokenResponse(token, lifetime);
        expectValidation(token, "client-id", lifetime);
    }

    @Test
    void fetchesExpandedMetadataAndDefaultsMissingCompanyFlags() {
        expectToken("token-one", 7200);
        igdb.expect(requestTo("https://api.igdb.com/v4/games"))
                .andExpect(header("Client-ID", "client-id"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-one"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("where id = (1942);")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("game_engines.name")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("game_type.type")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("game_type.name"))))
                .andRespond(withSuccess("""
                        [{"id":1942,"name":"Example","game_type":{"id":0,"type":"Main Game"},
                          "cover":{"image_id":"co_example"},
                          "involved_companies":[{"id":10,"company":{"id":20,"name":"Studio"},"developer":true}]}]
                        """, MediaType.APPLICATION_JSON));
        var games = client.fetchGames(List.of(1942L));
        assertEquals("co_example", games.getFirst().cover().imageId());
        assertEquals("Main Game", games.getFirst().gameType().name());
        assertTrue(games.getFirst().involvedCompanies().getFirst().developer());
        assertFalse(games.getFirst().involvedCompanies().getFirst().publisher());
        twitch.verify();
        igdb.verify();
    }

    @Test
    void refusesIncompleteGameResponses() {
        expectToken("token-one", 7200);
        igdb.expect(requestTo("https://api.igdb.com/v4/games"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertThrows(IgdbIntegrationException.class, () -> client.fetchGames(List.of(1942L)));
        igdb.verify();
    }

    @Test
    void retriesTransientCatalogFailureWithinABound() {
        expectToken("token-one", 7200);
        igdb.expect(requestTo("https://api.igdb.com/v4/games"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        igdb.expect(requestTo("https://api.igdb.com/v4/games"))
                .andRespond(withSuccess("[{\"id\":1942,\"name\":\"Example\"}]", MediaType.APPLICATION_JSON));
        assertEquals(1942L, client.fetchGames(List.of(1942L)).getFirst().id());
        twitch.verify();
        igdb.verify();
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

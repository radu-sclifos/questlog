package com.radusclifos.questlog.integration.igdb;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class IgdbClient {

    private static final String GAME_FIELDS = """
            fields name,summary,storyline,first_release_date,updated_at,
            game_type.type,cover.image_id,platforms.name,genres.name,game_engines.name,
            franchises.name,
            involved_companies.company.name,involved_companies.developer,
            involved_companies.publisher,involved_companies.porting,involved_companies.supporting,
            version_title,parent_game,version_parent,dlcs,expansions,standalone_expansions,
            remakes,remasters,ports,expanded_games,forks;
            """.replaceAll("\\s+", " ").strip();

    private final RestClient igdb;
    private final IgdbProperties properties;
    private final TwitchAppTokenProvider tokens;
    private long lastRequestNanos;

    public IgdbClient(RestClient igdb, IgdbProperties properties, TwitchAppTokenProvider tokens) {
        this.igdb = igdb;
        this.properties = properties;
        this.tokens = tokens;
    }

    /**
     * Read-only authentication probe; does not fetch or persist game records.
     */
    public void verifyAuthentication() {
        var response = request("games/count", "where id = 0;", CountResponse.class);
        if (response == null || response.count() == null || response.count() < 0) {
            throw new IgdbIntegrationException("IGDB returned an invalid authentication probe response.");
        }
    }

    public List<IgdbGame> fetchGames(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 100
                || ids.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("Provide 1 to 100 distinct positive IGDB IDs");
        }
        var query = GAME_FIELDS + " where id = (%s); sort id asc; limit %d;"
                .formatted(ids.stream().map(String::valueOf).collect(Collectors.joining(",")), ids.size());
        var response = request("games", query, IgdbGame[].class);
        if (response == null || response.length != ids.size()
                || Arrays.stream(response).anyMatch(game -> game == null || game.id() == null)
                || !Arrays.stream(response).map(IgdbGame::id).collect(Collectors.toSet()).equals(new HashSet<>(ids))) {
            throw new IgdbIntegrationException("IGDB did not return every requested game ID: " + ids);
        }
        return List.of(response);
    }

    public List<IgdbReference> fetchGenres() {
        return fetchDictionary("genres", "name");
    }

    public List<IgdbReference> fetchGameTypes() {
        return fetchDictionary("game_types", "type");
    }

    // Reminder: IGDB permits a maximum of 500 records per request
    private List<IgdbReference> fetchDictionary(String path, String nameField) {
        var result = new java.util.ArrayList<IgdbReference>();
        int offset = 0;
        while (true) {
            var page = request(path, "fields %s; sort id asc; limit 500; offset %d;".formatted(nameField, offset),
                    IgdbReference[].class);
            if (page == null || page.length > 500 || Arrays.stream(page).anyMatch(item -> item == null
                    || item.id() == null || item.id() < 0 || item.name() == null || item.name().isBlank())) {
                throw new IgdbIntegrationException("IGDB returned an invalid " + path + " dictionary");
            }
            result.addAll(List.of(page));
            if (page.length < 500) {
                return List.copyOf(result);
            }
            offset += page.length;
        }
    }

    /**
     * Fetches a released-game page using an ID cursor and a frozen sync window.
     */
    public List<IgdbGame> fetchCatalogGames(long since, long until, long afterId, int pageSize) {
        if (since < 0 || until <= 0 || since > until || afterId < 0 || pageSize < 10 || pageSize > 500) {
            throw new IllegalArgumentException("Invalid catalog sync window or page cursor");
        }

        String changes = since == 0 ? "" :
                " & ((created_at >= %d & created_at <= %d) | (updated_at >= %d & updated_at <= %d) | first_release_date >= %d)"
                        .formatted(since, until, since, until, since);
        String query = GAME_FIELDS + " where id > %d & first_release_date != null & first_release_date <= %d%s; sort id asc; limit %d;"
                .formatted(afterId, until, changes, pageSize);
        var response = request("games", query, IgdbGame[].class);
        if (response == null || response.length > pageSize) {
            throw new IgdbIntegrationException("IGDB returned an invalid catalog page");
        }

        long previous = afterId;
        for (var game : response) {
            if (game == null || game.id() == null || game.id() <= previous || game.firstReleaseDate() == null
                    || game.firstReleaseDate() > until) {
                throw new IgdbIntegrationException("IGDB returned an invalid catalog page order or release date");
            }
            previous = game.id();
        }
        return List.of(response);
    }

    private <T> T request(String path, String body, Class<T> responseType) {
        int authenticationRetries = 0;
        int transientRetries = 0;
        while (true) {
            var token = tokens.accessToken();
            try {
                throttle();
                return igdb.post().uri(path)
                        .header("Client-ID", properties.clientId())
                        .headers(headers -> headers.setBearerAuth(token))
                        .contentType(MediaType.TEXT_PLAIN)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve().body(responseType);
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().value() == 401) {
                    tokens.invalidate(token);
                    if (authenticationRetries++ == 0) {
                        continue;
                    }
                }
                if ((path.equals("games") || path.equals("genres") || path.equals("game_types"))
                        && retryable(exception.getStatusCode()) && transientRetries++ < 2) {
                    pause(retryDelay(exception, transientRetries));
                    continue;
                }
                throw new IgdbIntegrationException("IGDB request failed (HTTP " + exception.getStatusCode().value() + ")."
                        + badRequestDetail(exception, token));
            } catch (RestClientException exception) {
                throw new IgdbIntegrationException("IGDB request failed; check connectivity and response format.");
            }
        }
    }

    /**
     * Exposes only structured query diagnostics, with credentials removed.
     */
    private String badRequestDetail(RestClientResponseException exception, String token) {
        if (exception.getStatusCode().value() != 400) {
            return " Check credentials, rate limits and provider availability.";
        }
        try {
            var root = JsonMapper.builder().build().readTree(exception.getResponseBodyAsString());
            var error = root.isArray() ? root.path(0) : root;
            var details = new StringBuilder();
            for (var field : List.of("title", "cause", "message")) {
                var value = error.path(field);
                if (value.isString()) details.append(value.asString()).append("; ");
            }
            var sanitized = details.toString();
            for (var secret : List.of(token, properties.clientSecret(), properties.clientId())) {
                if (!secret.isEmpty()) sanitized = sanitized.replace(secret, "[REDACTED]");
            }
            sanitized = sanitized.replaceAll("[\\p{Cntrl}]", " ").strip();
            if (!sanitized.isEmpty()) {
                return " Query rejected: " + sanitized.substring(0, Math.min(500, sanitized.length()));
            }
        } catch (RuntimeException ignored) {
            // Do not attach raw response bodies or HTTP exceptions.
        }
        return " Query rejected; IGDB returned no readable structured diagnostic.";
    }

    private synchronized void throttle() {
        long remaining = lastRequestNanos == 0 ? 0 : 350_000_000L - (System.nanoTime() - lastRequestNanos);
        if (remaining > 0) {
            pause((remaining + 999_999) / 1_000_000);
        }
        lastRequestNanos = System.nanoTime();
    }

    private boolean retryable(HttpStatusCode status) {
        return status.value() == 429 || status.is5xxServerError();
    }

    private long retryDelay(RestClientResponseException exception, int attempt) {
        var value = exception.getResponseHeaders() == null ? null
                : exception.getResponseHeaders().getFirst("Retry-After");
        try {
            if (value != null) {
                return Math.max(1, Math.min(30, Long.parseLong(value))) * 1000;
            }
        } catch (NumberFormatException ignored) {
            // Do nothing
        }

        return attempt * 1000L;
    }

    private void pause(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IgdbIntegrationException("IGDB request interrupted.");
        }
    }

    private record CountResponse(Long count) {
    }

}

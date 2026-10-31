# Spring Batch infrastructure and IGDB authentication

This step installs Spring Batch 6.0.6 (managed by Spring Boot 4.1.2) with a JDBC
JobRepository in the application's PostgreSQL database. Flyway V1 creates the
official Batch metadata tables and sequences. Boot schema initialization is
disabled so Flyway is the sole schema owner.

The catalog synchronization job is documented in CATALOG_IMPORT.md. No scheduler or
maintenance window is enabled. spring.batch.job.enabled=false prevents Boot from
launching jobs implicitly; the catalog's explicit opt-in runner is separate.
No @EnableBatchProcessing is needed: Boot configures the
infrastructure through spring-boot-starter-batch-jdbc.

## Register the application once

1. Sign in to Twitch and enable two-factor authentication on your account.
2. Open https://dev.twitch.tv/console/apps and register an application.
3. Set the OAuth redirect URL to http://localhost. The client credentials flow
   does not use this URL: QuestLog receives no login callback.
4. Select Confidential as client type.
5. Open Manage, copy Client ID and generate a Client Secret with New Secret.

These credentials identify QuestLog itself. Users of QuestLog do not need Twitch
accounts and do not authorize access to their personal data.

Official setup: https://api-docs.igdb.com/#account-creation
OAuth flow: https://dev.twitch.tv/docs/authentication/getting-tokens-oauth/#client-credentials-grant-flow

## Configure your backend process

Set these environment variables in the IDE's Run Configuration or in your
deployment's secret configuration:

    IGDB_ENABLED=true
    IGDB_CLIENT_ID=<your Twitch application's client ID>
    IGDB_CLIENT_SECRET=<your Twitch application's client secret>
    IGDB_VERIFY_ON_STARTUP=true

Keep the existing DB_URL, DB_USERNAME and DB_PASSWORD configuration.
Never commit the real values or send the secret to the frontend.
A .env file is not automatically loaded by Spring Boot.

Start QuestLog normally, for example from backend/:

    ./mvnw spring-boot:run

On Windows:

    .\mvnw.cmd spring-boot:run

With verification enabled, startup:

1. Applies pending Flyway migrations.
2. Requests an app access token from Twitch using a form POST.
3. Validates the token and checks its client ID.
4. Sends an authenticated, read-only POST to IGDB /v4/games/count with
   where id = 0;. It retrieves no game records.
5. Logs:

    IGDB authentication verified successfully; no games imported.

If the check fails, application startup fails with a sanitized error. Disable
IGDB_VERIFY_ON_STARTUP after the manual check if provider outages should not
prevent the website from starting. IGDB_ENABLED can remain true: token acquisition
is lazy, on first use. With IGDB_ENABLED=false (the default), the backend requires
no provider credentials and creates no IGDB client.

## Token lifecycle

- The client secret is sent only to Twitch, in the form body.
- IGDB receives Client-ID and Authorization: Bearer <token>.
- The token is cached only in memory and shared safely between callers.
- It is renewed on demand before expiry, with a 60-second margin.
- Cached tokens are revalidated on use after an hour. There is no background
  polling or Twitch user session.
- An IGDB 401 invalidates the cached token, acquires a new one and retries once.
- A 429, timeout or provider failure stops the probe; it does not loop or import.
- Provider error bodies and raw HTTP exceptions are not attached to public
  integration exceptions, to avoid logging credentials or tokens.
- HTTP connect/read timeouts default to 5/15 seconds. Avoid enabling HTTP
  wire/body logging for the authentication client.

No refresh token is required for app credentials. A new app token is requested
with the same client credentials when necessary.

## Troubleshooting

- Missing configuration: enabled integration requires both credentials.
- Twitch 400/401/403: check Client ID, Client Secret and Confidential app type.
- IGDB 401 after retry: check application credentials and provider access.
- IGDB 429: wait before retrying; the probe does not bypass rate limits.
- Database startup failure: fix datasource/Flyway before the authentication check.
- Connectivity failure: check outbound HTTPS access to id.twitch.tv and
  api.igdb.com.

## Automated verification

From backend/:

    ./mvnw -Dtest=IgdbAuthenticationTests,IgdbConfigurationTests,BatchInfrastructureTests test

HTTP tests simulate Twitch/IGDB, including expiry, concurrent callers, validation,
401 recovery, rate limits, malformed responses and secret-safe errors.
The Batch test uses H2 in PostgreSQL compatibility mode and executes the exact V1
baseline SQL, then writes and reads a job instance through the JDBC repository.
It does not replace checking all Flyway migrations against a real PostgreSQL
test database. A real credential check is opt-in and is not run by these tests.

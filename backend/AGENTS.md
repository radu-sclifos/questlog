# QuestLog backend — agent guide

Reviewed on November 7, 2026. Paths are relative to `backend/`.
Source code takes precedence over this guide: inspect relevant files before editing.

## Architecture and conventions

- Java 25, Spring Boot 4.1.2, Spring MVC, Spring Security, Spring Data JPA,
  PostgreSQL, Flyway, and Spring Batch. See `pom.xml` for versions.
- A monolith with APIs and IGDB imports in the same application and database.
  Packages are organized by feature, with controllers, services, repositories, DTOs, and mappers.
- Controllers handle HTTP contracts and DTO validation; services handle application
  rules and transactions; repositories handle data access. Return DTOs in API responses.
- MapStruct and Lombok generate code: edit source definitions, never generated files.
  Mappers using `unmappedTargetPolicy = ERROR` require explicit mappings or ignores.
- With `open-in-view: false`, read lazy associations and map them to DTOs within
  the appropriate transaction. Avoid introducing N+1 queries.
- Explicit application errors use `ResponseStatusException`.

## Quick map

Java root: `src/main/java/com/radusclifos/questlog/`.

| Area | Responsibility |
| --- | --- |
| `QuestlogApplication.java` | Bootstrap and JPA repositories with a shared base implementation |
| `config/SecurityConfig.java` | Sessions, BCrypt, CSRF, and HTTP access rules |
| `auth/controller/AuthController.java` | CSRF token endpoint |
| `user/controller/` | Current user, public profiles, suggestions, avatars, and favorites |
| `user/service/` | `UserService`, `ProfileService`, `AvatarService` |
| `entry/` | Personal entries, CRUD, search, ownership, and mapping |
| `game/` | Shared catalog, details, suggestions, platforms, and relations |
| `shared/search/` | JPA criteria, validation, filters, sorting, and pagination |
| `integration/igdb/` | IGDB client, Twitch app credentials, and token lifecycle |
| `batch/catalog/` | Import launching, scheduling, history, recovery, and persistence |
| `batch/catalog/tasklet/` | Dictionary refresh, game imports, and relation resolution |
| `src/main/resources/application.yml` | Configuration and environment variables |
| `src/main/resources/db/migration/` | Application schema and Batch metadata managed by Flyway |
| `src/test/java/` | Tests organized by area |

## Invariants and contracts to preserve

- The context path is `/api`. Controller and security-chain paths do not include it.
- Authentication uses sessions: form-urlencoded login at `/auth/login`, logout at
  `/auth/logout`, tokens from `/auth/csrf`, and the current user from `/me`. Do not replace with JWT.
- The security chain covers auth, me, users, and entries. Public exceptions include
  entry search POST requests and GET requests for user suggestions, public profiles,
  and avatars. `/games/**` is outside the current chain: catalog endpoints perform
  reads, including POST search. Review security before adding mutations.
- Entry ownership is assigned by the server. Individual entry access uses both
  the entry ID and authenticated user; another user's entry returns 404.
- Library search is public and selects the user by username; responses include
  notes. Private libraries are not implemented.
- Entries reference catalog games through a required `gameId`; they have no
  independent title. Updates cannot change the associated game.
- User/game uniqueness is enforced in the database; the service maps the relevant
  constraint violation to 409. Do not replace concurrent-write protection with only a preflight query.
- If provided, `platformId` must belong to the game. Achievement counts are optional
  and nonnegative; unlocked cannot exceed total when both are present.
- Ratings are optional and range from 0 to 10; notes allow up to 2000 characters.
  Do not assume the backend rejects negative playtime: `SaveEntryRequest` declares no minimum constraint.
- `EntryDto` includes a compact `EntryGameDto`; do not load the full `GameDto` for entries.
- Search runs in the database before pagination. Paths use entity names, such as
  `game.name`. Preserve client-criteria validation, the server-side user filter,
  and stable sorting; defaults live in `EntrySearchDefaults`.
- Public profiles include statistics, currently playing titles, and favorites.
  Personal updates use `/me`, `/me/favorites`, and `/me/avatar`. Registration and
  password recovery endpoints are not currently implemented.
- Avatars are normalized to 256 × 256 PNG images in `user_avatars`; revisions support caching/ETags.
- The IGDB importer writes the catalog; there is no game-creation endpoint.
  Preserve internal IDs, shared associations, and personal data during upserts.
- Import execution and recovery assume a single backend instance. The local guard
  is not a distributed lock. Scheduling is opt-in and runs at 03:00 `Europe/Rome`.

## Configuration, schema, and verification

- Database configuration uses `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`; provider
  integration and imports are opt-in. See `application.yml` and the documents below.
  Spring Boot does not automatically load a `.env` file.
- Flyway owns the schema; Hibernate uses `ddl-auto: validate`. The consolidated V1
  baseline requires an empty database. For existing migrated databases, add versioned
  migrations rather than rewriting applied migrations.
- From the backend directory: `.\mvnw.cmd test` and `.\mvnw.cmd package` (includes tests).
  On Unix systems, use `./mvnw`.
- The suite combines isolated tests and PostgreSQL integration tests using Testcontainers.
  Java 25 and a running Docker-compatible Linux container runtime are required for
  the full suite; no DB_URL, DB_USERNAME, DB_PASSWORD or IGDB credentials are needed.
  All tests run in the Maven test phase; package includes the same suite.
- JaCoCo generates HTML/XML coverage in `target/site/jacoco/` after successful tests.
  Use `clean test` for a fresh full-suite measurement. Maven's JaCoCo check warns in
  the logs below 80% overall line coverage without failing the build, locally and in CI.
  No branch-coverage rule or custom CI coverage summary is configured.
- Add @PostgresTest alongside @SpringBootTest to database integration tests. It imports
  a container-backed service connection, activates the test profile and closes the
  context after each class. Flyway runs production migrations; Hibernate validates them.
  See [Testing](docs/TESTING.md) for isolation, commands and the backend CI workflow.
- Select tests in PowerShell with `.\mvnw.cmd '-Dtest=SomeTest,AnotherTest' test`.
  Choose tests relevant to the change; documented commands do not certify previous results.
- Simulated provider calls do not verify real credentials or the behavior of the live service.

## Further reading

- [IGDB authentication and Batch infrastructure](docs/IGDB_AUTHENTICATION.md)
- [Catalog model, search, and REST contracts](docs/GAME_CATALOG.md)
- [Imports, scheduling, recovery, and operational limits](docs/CATALOG_IMPORT.md)

For changes spanning both applications, also check [the frontend guide](../frontend/AGENTS.md),
Angular models, and services. Update relevant documentation without duplicating detailed explanations.

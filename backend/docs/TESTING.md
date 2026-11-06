# Backend testing

## Running the suite

Requirements: Java 25 and a running Docker-compatible runtime supporting Linux
containers (for example Docker Desktop). Maven is provided by the wrapper.
The first run downloads Maven dependencies and container images.

From `backend/`, run:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

On Linux/macOS, use `./mvnw`. `package` runs the tests before creating the executable
JAR. All tests run through Surefire in the `test` phase; there is no separate
integration-test phase, Failsafe configuration or mandatory `verify` command.

To select tests in PowerShell:

```powershell
.\mvnw.cmd '-Dtest=EntryGameIntegrationTests,GameCatalogIntegrationTests' test
.\mvnw.cmd '-Dtest=IgdbAuthenticationTests,EntryControllerTests' test
```

The second selection contains only isolated tests and does not require Docker.
If Docker is unavailable, database tests fail rather than being silently skipped.

## Test layers and database lifecycle

- Isolated tests use JUnit, Mockito, MockMvc or simulated HTTP responses. They
  verify application rules, HTTP contracts, security and provider error handling.
- Spring application integration tests use `@SpringBootTest` plus `@PostgresTest`.
  The latter imports `PostgresTestConfiguration`, activates the `test` profile and
  marks the context dirty after each class. Spring Boot manages a PostgreSQL
  container bean and injects its connection details via `@ServiceConnection`.
- Each application test class starts with its own empty database. Spring closes
  the context and container after the class, so committed records cannot leak to
  another class. Within a class, transactional tests roll back; tests exercising
  real commits must use distinct fixtures or explicitly clean up between methods.
- The focused Batch infrastructure test owns a separate container in a
  try-with-resources block and applies Flyway programmatically.

The image is pinned to `postgres:17.7-alpine` in `PostgresTestConfiguration`;
update that one factory when changing the tested PostgreSQL version. Match the
production major when choosing the deployment database. No container reuse or
fixed host port is configured. Testcontainers cleans up its temporary resources.

Database tests apply the production `db/migration` scripts through Flyway.
Application contexts retain Hibernate `ddl-auto: validate`; Hibernate does not
create a substitute schema. Baseline contract tests check schema constraints on
PostgreSQL; they do not currently model an upgrade from a previous release.

No development database or `DB_*` environment variables are needed: the service
connection supplies the container endpoint and temporary credentials. The test
profile disables provider integration and automatic/scheduled imports. The import
job test explicitly enables IGDB configuration with dummy values and replaces
`IgdbClient` with a mock. HTTP tests simulate Twitch/IGDB; they make no live calls.

When adding a database test, use `@PostgresTest` and `@SpringBootTest` together.
Keep transaction-boundary tests outside a surrounding test transaction so that
commit behavior and DTO access after transaction closure are actually exercised.

## Continuous integration

`.github/workflows/backend-ci.yml` builds the backend on pushes to `main` that
change `backend/**` or the workflow itself, and supports manual dispatch.
Feature-branch pushes and pull request events do not trigger it. A failed run
reports the problem after integration; it does not revert commits or configure
any required pull request checks.

It installs Java 25, caches Maven
dependencies and runs the wrapper's `package` command on a Linux runner. The
runner's Docker daemon is used directly by Testcontainers; no separate PostgreSQL
service or database/provider secrets are required.

Surefire reports are uploaded even when tests fail; a successful build uploads
the executable JAR. This workflow validates and packages the backend. Deployment
requires a separately configured target environment and is not part of this job.

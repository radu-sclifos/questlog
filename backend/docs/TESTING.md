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

## Code coverage

JaCoCo is attached to the test JVM by Maven's `prepare-agent` goal. Its `report`
goal runs in the `test` phase after Surefire, producing HTML and XML without
requiring `verify`. Both `test` and `package` generate the reports on success.

For a fresh full-suite measurement, with Docker running:

```powershell
.\mvnw.cmd clean test
```

Open `target/site/jacoco/index.html` in a browser. Keep the entire report folder
together: the HTML pages reference assets and package-level reports within it.
`target/site/jacoco/jacoco.xml` is available for future tooling such as SonarQube;
no SonarQube connection is configured. Raw execution data is in `target/jacoco.exec`.

Coverage is informational: no coverage-based build failure is configured.
After report generation, Maven runs `jacoco:check` in the `test` phase with an
overall (`BUNDLE`) `LINE` / `COVEREDRATIO` minimum of `0.80`. Below 80%, JaCoCo
logs warnings; `haltOnFailure=false` allows the build to continue. This applies
locally and in CI; exactly 80% satisfies the rule. No branch-coverage threshold
is configured. The full JaCoCo report still includes its standard metrics.
There is no custom GitHub Summary or warning annotation step; inspect Maven's
logs for the check outcome and download the artifact for the complete report.
Coverage shows execution, not assertion quality.
No project-specific report exclusions are configured.

Use `clean` when switching between full and selected test runs: execution data
can otherwise accumulate across invocations. A selected run measures only that
selection, not the full suite. If Surefire fails, Maven stops before the report
goal; after inspecting the failures, `./mvnw jacoco:report` can render any execution
data collected by that run. An IDE test launch uses its own coverage configuration;
the Maven plugin applies to Maven executions.

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

`.github/workflows/backend-ci.yml` builds the backend on every pull request
targeting `main` and on manual dispatch. There are no path filters, so pull
requests also run the check for frontend-only and documentation changes.
New commits pushed to an open pull request rerun the check. Pushes to `main`
and feature-branch pushes without an open pull request do not trigger it.

Merge protection is configured separately in GitHub, not by the workflow file.
In repository Settings, configure a branch protection rule for `main` requiring
a pull request and the `build` status check from GitHub Actions before merging.
For a solo project, required approving reviews can remain disabled. Select the
check after it has appeared in a workflow run, and verify the rule is active.
Do not add a pull-request path filter: a skipped required workflow can leave
the check pending and block unrelated pull requests. Avoid CI-skip directives
in pull-request commit messages for the same reason. See GitHub's
[required-check troubleshooting](https://docs.github.com/en/pull-requests/how-tos/merge-and-close-pull-requests/troubleshooting-required-status-checks).

It installs Java 25, caches Maven
dependencies and runs the wrapper's `package` command on a Linux runner. The
runner's Docker daemon is used directly by Testcontainers; no separate PostgreSQL
service or database/provider secrets are required.

Surefire reports are uploaded even when tests fail; a successful build uploads
the executable JAR and the complete HTML/XML coverage folder as the
`backend-coverage` artifact, retained for 14 days. Download and extract that
artifact, then open `index.html` to browse coverage locally.
This workflow validates and packages the backend. Deployment
requires a separately configured target environment and is not part of this job.

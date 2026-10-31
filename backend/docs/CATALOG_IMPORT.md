# Catalog synchronization

With max-pages set to 0 (the default), the first successful run imports the catalog,
including future games and games without a release date. Later runs query only
games created or updated since the previous successful catalog run started, minus
a five-minute overlap. The upper boundary for created/updated timestamps is frozen
at launch (UTC epoch seconds). Release dates do not control import eligibility.

The reference is read from COMPLETED catalog executions in Spring Batch's existing
metadata tables. Failed/stopped/running executions and page-limited test runs do not
advance it. The reference refresh, games and relations steps must all succeed. No schema change is
required. Preserve Batch execution history: deleting it causes another initial
catalog import. Existing local games are upserted, not duplicated.

Each page uses `id > lastGameId`, sorted ascending. This avoids offset shifts while
the provider catalog changes. IGDB does not provide a transaction-wide snapshot;
changes after the frozen boundary are picked up by a later run. The overlap is a
practical tolerance, not a guarantee against arbitrary provider indexing delays
or clock skew. A first full import may be large; there is no popularity filter yet.
Deletion reconciliation and independent dictionary sync have not been added.
Related metadata is still refreshed when an imported game is written.

Set `CATALOG_IMPORT_SCHEDULED=true` and `IGDB_ENABLED=true` to synchronize daily
at 03:00 in `Europe/Rome` (including daylight saving time). The application must
remain running; missed executions while it is stopped are not recovered.
Scheduling is disabled by default and independent of `CATALOG_IMPORT_ON_STARTUP`.
Nightly runs use `CATALOG_IMPORT_PAGE_SIZE` and always have unlimited pages,
ignoring the startup/test `CATALOG_IMPORT_MAX_PAGES` limit. They reuse the same
incremental history and concurrency guard as startup imports. Failures are logged;
the next scheduled run retries from the last successful synchronization.
Use a single application instance for scheduling; there is no distributed lock.

For a small test dataset, set CATALOG_IMPORT_MAX_PAGES to a positive number, such
as 20. The same catalog query, filters, writes and sync time window are used; the
games step simply stops after 20 nonempty provider pages, without requesting page
21. Games excluded by the provider-side query do not consume page slots. Any records rejected by the defensive local check still count towards the page limit. If IGDB runs out of
games earlier, the step finishes immediately. The relations step still runs.

A limited run can finish COMPLETED but never advances the full catalog sync
reference, even if it happens to exhaust the available pages. Repeating it starts
again from the first matching page. Setting max-pages back to 0 performs a normal
full/incremental sync from the last successful unlimited run. Specific game IDs
are no longer configured. This limit is for game pages, not relation pages.

The catalog query excludes bundles, mods (including standalone mods), and editions
before pagination, reducing HTTP requests and response payloads. Game types are
filtered using game_type.type labels; absent types remain eligible. Editions are
excluded when version_parent is nonzero or version_title is nonempty.
A defensive local check remains for direct ID lookups and unexpected provider data;
it additionally normalizes type labels and treats whitespace-only version titles
as blank. Local filtered counts therefore do not include provider-side exclusions.
Remakes, remasters and expansions remain eligible. Existing rows are not
automatically deleted by this import filter.

Bundle membership is not requested or stored; belonging to a bundle does not
exclude an otherwise valid game.

## Run manually

Keep the database and Twitch credentials already configured. Set these variables
in the backend's IDE run configuration or process environment:

    IGDB_ENABLED=true
    IGDB_VERIFY_ON_STARTUP=false
    CATALOG_IMPORT_ON_STARTUP=true
    CATALOG_IMPORT_MAX_PAGES=20
    CATALOG_IMPORT_PAGE_SIZE=250

The page size must be 10–500 (inclusive); 250 is the default when omitted.

Start the backend normally from backend/:

    .\mvnw.cmd spring-boot:run

Flyway creates the complete schema from V1 on an empty database. The explicit runner
starts catalogImportJob synchronously during startup, imports the matching
records and logs the execution ID, page progress and final status:

    Catalog import execution <id> finished with status COMPLETED.

If the job or launch fails, the runner logs the error and lets the backend keep
running. Batch execution history and already committed pages are retained. No background work remains after completion.
Set CATALOG_IMPORT_ON_STARTUP=false afterward so normal website restarts do not
repeat the import. spring.batch.job.enabled remains false.
No public HTTP endpoint for starting jobs has been added.

Each nonempty game page logs fetched/saved/filtered/skipped counts and elapsed times in
milliseconds: igdbMs includes the provider request, throttling, retries and JSON
deserialization; dbMs covers writePage, including flush and transaction commit;
pageMs covers both plus in-tasklet bookkeeping, excluding the log itself and
Spring Batch's subsequent checkpoint persistence. Dictionary initialization logs
its own igdbMs and dbMs separately. The final empty game page logs its fetch time.

Set CATALOG_IMPORT_DIAGNOSTICS=true before starting the backend to append
sqlStatements and flushes to each nonempty game-page log. These are deltas of
Hibernate's prepared-statement and flush counters around writePage, including
commit, excluding dictionary initialization and Batch checkpoint persistence.
sqlStatements counts prepared SQL statements, including reads and writes, rather
than only SELECT queries or individual rows. The counters are SessionFactory-wide:
avoid concurrent application traffic for a meaningful import measurement. They
are never reset by the importer. Disabled diagnostics show disabled rather than
misleading zero values; turn the option off after profiling to avoid the overhead
of collecting global Hibernate statistics. Flushes count Hibernate's completed
full flushes, not every automatic partial-flush check or every SQL statement.

## Steps and persistence

CatalogImportConfiguration assembles the job and steps. Their execution logic
lives in CatalogRefreshReferencesTasklet, CatalogImportGamesTasklet and CatalogImportRelationsTasklet under
batch.catalog.tasklet. CatalogImportParameters parses the persisted job parameters
shared by the validator and the games tasklet.

CatalogImportWriter coordinates page persistence using Spring Data repositories.
CatalogReferenceResolver provides explicit lookups for shared dictionaries.
EntityManager is used only for flush/clear; the page transaction is unchanged.

Each game page preloads existing games (including referenced relation targets),
platforms, engines, franchises and companies using at most one lookup query per
entity type, restricted to the IDs in eligible page records. Empty ID sets do not
issue queries. The maps are local to the page transaction and newly created
records are added immediately so later records in that page reuse them. Names
are still updated in source order. Company involvement rows and outgoing links
are still replaced, but deletion happens in three bulk statements at the start
of the page, before managed entities are changed. Only existing eligible source
games are included, never filtered games or targets merely referenced by them.
New games need no deletion. Per-game forced flushes are removed; the final
explicit flush and Hibernate's automatic flushing remain. The deleted-link
collections must stay unloaded because bulk deletes bypass the persistence
context. Deletes and new writes share the page transaction and roll back together.
No page maps survive completion or rollback, and no full company/platform/engine/
franchise tables are cached. This reduces lookup round trips even if every
company in the page is different; it does not eliminate the association writes.

Before the first game page of each execution, the importer fetches the complete
genres and game_types dictionaries (normally one request each, with pagination
if necessary). HTTP calls run outside database transactions. Both dictionaries
are upserted together; only after commit does the resolver replace its two maps
of Genre and GameType entities. Game pages reuse these records for associations,
without dictionary lookup queries or name updates from expanded game metadata.
Unknown genre IDs are skipped; unknown game type IDs leave the game type empty.
Existing dictionary rows absent from the provider response are retained in the
database but excluded from the new maps. A dictionary fetch or persistence failure
fails the import before any game page is processed.

1. catalogImportGames requests one catalog page, then updates the catalog
   in its own short transaction. No database transaction is held during HTTP.
   The last game ID and processed page count are persisted after a successful page.
   Relations to existing local targets are created immediately.
2. catalogImportRelations scans pending links in bounded pages, creates missing
   local relations when the targets become available, and deletes the resolved
   pending rows in the same transaction. Unresolved rows remain for future runs.

Imported fields include name, full UTC-derived release date, summary, storyline,
cover image ID, game type, platforms, genres, companies and their roles, engines,
franchises and series (from IGDB collections). Edition titles are used only for eligibility filtering; source
update timestamps are not persisted on games.
Internal game IDs survive updates. Matching uses IGDB ID, never title.
Nullable metadata and provider-owned associations are replaced by the new source
snapshot, including removals. Personal entries and unrelated catalog games are
not modified or deleted. Shared reference records are retained.
cover_image_id is stored and exposed as coverImageId by the REST API. Clients
derive the IGDB image URL using the desired size; no full URL is stored.

PendingGameRelation maps to pending_game_relations and stores only references
whose target is not present yet. game_relations stores the resolved local links.
Updating a source reconciles both its resolved and pending outgoing references.
The importer does not recursively expand the requested game set. Raw parent
references and explicit typed links can coexist; they retain their source
direction. Similar-game recommendations are excluded.

## Failure behavior

Every launch creates a new job instance and reads the last successful catalog
sync start from Batch history. Failed runs never advance that reference. A later
attempt starts from the beginning of the resulting time window, not from the
failed page. There is no manual execution-ID restart configuration.

The normal page write is atomic. If it fails because of invalid record data
(validation, date conversion, or database data/constraint errors), the page is
rolled back and eligible games are retried individually in independent transactions.
A failing game is rolled back and skipped; its IGDB ID, name and error are logged.
Existing data and outgoing links for that game are preserved. Valid games still
commit, and pending links to absent skipped games remain unresolved.
The games step records write skips separately from eligibility filters. Pages
containing only skipped games still advance the cursor and consume the page limit.
A completed unlimited run advances sync history even with skipped records; no
automatic retry queue is maintained. An unchanged skipped game may never be
requested again by incremental synchronization.

Provider request failures, dictionary refresh failures, database availability
errors and unexpected runtime failures still fail the job.
Already committed pages (or individual retries) remain after a failure and may
be processed again on the next attempt. Upserts and relationship replacement are
idempotent, so replay does not duplicate games or relationships. A failed initial
import is retried as a full import until one complete run succeeds.

Missing expanded names for dictionaries resolved per game or invalid game names
cause the individual game to be skipped. Invalid provider page identities/order
are rejected before writing and still fail the job.
Genres and game types use the preloaded dictionaries instead; unknown references
are ignored without failing the page.

The launcher rejects overlapping imports within the single backend instance.
After acquiring that local guard, it recovers persisted unfinished catalog
executions left by an abrupt shutdown: active steps and their job are marked
FAILED together in a transaction, with an end time and an explanatory exit
message. Completed steps are preserved. Each recovered execution is logged as
an error. Recovery runs before both startup and scheduled imports, and does not
advance the last successful sync reference. If recovery itself fails, no new job
is launched; the startup runner and scheduler log the error without stopping the
backend.

Automatic recovery assumes exactly one backend instance owns this catalog job.
The local guard does not distinguish a crashed job from one executing on another
server. Multiple backend instances would require a shared lock before recovery
and for the full duration of every import; they are not supported here.

## Provider traffic

IGDB calls are spaced by at least 350 ms per client instance. Catalog reads
retry 429 and 5xx at most twice with bounded backoff (numeric Retry-After is
honored up to 30 seconds). A 401 renews the token and retries once.
Transport timeouts and invalid payloads fail the page and can be retried in a new execution.
Credentials, access tokens and raw provider error bodies are not logged.

## Verification

    .\mvnw.cmd -Dtest=CatalogImportJobTests,CatalogImportParametersTests,IgdbAuthenticationTests,IgdbConfigurationTests,BatchInfrastructureTests test

CatalogImportJobTests boots the real application against H2 in PostgreSQL mode,
applies the schema with Flyway and validates the Hibernate mappings. IGDB is simulated.
It checks initial import, UTC date conversion, shared dictionaries, updates,
relationship removal/deferred resolution, incremental history, failed-run isolation,
retries from the beginning without duplicating committed records, exact page limits,
filtered-page counting and isolation of limited runs from the full sync history.
HTTP tests check expanded payloads, incomplete responses and transient retries.
A live PostgreSQL/IGDB execution must still be verified with real configuration.

The job starts with `catalogRefreshReferences`, a dedicated tasklet that fetches
genres and game types outside a database transaction and refreshes their tables
and caches atomically before `catalogImportGames`. A refresh failure stops the job
before game import. Other reference types remain refreshed per game page.


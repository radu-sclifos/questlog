# Game catalog — first persistence step

The catalog is independent from personal library entries. The incremental
Spring Batch importer is described in CATALOG_IMPORT.md; entries reference imported games through a required game_id.

## Game

Catalog naming uses standalone domain names and retains Game only for types and
associations that need that context:

| Java entity | SQL table |
| --- | --- |
| Game | games |
| Engine | engines |
| Genre | genres |
| Platform | platforms |
| Company | companies |
| Franchise | franchises |
| Series | series |
| GameType | game_types |
| GameCompany | game_companies |
| GameRelation | game_relations |

Simple join tables are game_engines, game_genres, game_platforms and
game_franchises and game_series.

- Internal generated ID and optional unique IGDB ID.
- Name, first release date (SQL DATE / Java LocalDate), summary and storyline.
- Game type and cover image ID.
- Platforms, genres, engines, franchises and series through join tables.
- Company involvements with developer, publisher, porting and supporting roles.
- Directed links to other imported games.

Engine is the Java entity stored in engines; Game.engines uses the game_engines
join table with game_id and engine_id. The provider field game_engines will be
mapped to this collection by the importer.

Series are imported from IGDB collections and remain distinct from franchises.
Both use page-scoped reference preloading and many-to-many associations.

Optional provider metadata stays nullable when IGDB does not supply it.
Games are created only by the IGDB importer, with no frontend creation endpoint.
Game names and cover image IDs use TEXT without an application-defined length limit.
Game names must still be nonblank.
The importer converts IGDB release timestamps using UTC.
Reference entities have internal IDs, optional unique IGDB IDs and names; game
types are data, not hardcoded IGDB enum numbers.

The persistent name replaces the old title column. The REST DTO returns name and coverImageId through its mapper. Clients construct cover URLs
from the image ID and desired size; the full URL is not persisted.
Criteria queries on Game use name; Entry uses game.name for the catalog title.

## Directed relations

GameRelation stores source, target and an application-owned enum:

- DLC, EXPANSION, STANDALONE_EXPANSION.
- REMAKE, REMASTER, PORT, EXPANDED_GAME, FORK.
- VERSION_OF: source is an edition of target.
- PARENT_GAME: target is the source game's parent in IGDB.

Self references and duplicate triples are rejected by PostgreSQL.
The first importer retains raw parent references alongside explicit typed links,
preserving the provider's direction. Similar-game recommendations are excluded.
Both endpoints must exist locally. pending_game_relations persists source references
whose targets are absent. Existing targets are linked immediately. The second
step resolves pending rows when possible and deletes them atomically, without
triggering recursive imports.

Associations are lazy and do not cascade removals into shared catalog data.
Persist reference rows, games, company involvements and relations explicitly in
that order. Inverse collections are read after reload; callers managing an
already loaded graph must also maintain its in-memory consistency.

## Database and verification

V1__baseline.sql creates the complete application, catalog, pending relations and Spring Batch schema from scratch. It contains the final table definitions and constraints, without historical ALTER or data migration statements. This consolidated history requires an empty database, including flyway_schema_history. Hibernate remains configured with ddl-auto=validate.

GameCatalogIntegrationTests checks the mapping, full release date, join tables,
company roles and relation direction against a configured PostgreSQL database.
Use a dedicated test database with DB_URL, DB_USERNAME and DB_PASSWORD set:

    ./mvnw -Dtest=GameCatalogIntegrationTests,CriteriaSearchIntegrationTests test

GameControllerTests verifies the game REST contract; GameRelationTests checks
self-reference rejection before persistence.

## Search paths

The shared search builder accepts dotted paths such as gameType.name, genres.id,
platforms.id and series.id. Each filter crossing a collection uses its own EXISTS:
IN matches any listed value, while repeated EQUAL filters require all values,
possibly on different collection elements. Empty collections do not satisfy these
filters; IS_NULL on a collection path checks a null field on an existing element.
Sorting accepts basic fields and singular paths, but rejects collection paths.
SearchIgnore excludes private fields from both filters and sorts.
POST /games/search accepts SearchCriteria and returns items, page, size,
totalElements and totalPages. Search fields use entity names (name, genres.id,
series.id). Response items use name rather than the previous title field.
GET /games/suggestions?name=Holl&limit=10 returns a list of suggestions containing the
text anywhere in the name, ignoring case. Names are trimmed and must not be blank;
limit defaults to 10 and must be at least 1. Results are sorted by name and
ID without a total count. Wildcards in the supplied name are treated literally.

## Game responses

Search and GET /games/{id} return the complete GameDto:
id, igdbId, name, firstReleaseDate, summary, storyline, coverImageId, gameType,
platforms, genres, engines, franchises, series, involvedCompanies and relations.
GenreDto, PlatformDto, EngineDto, FranchiseDto, SeriesDto, GameTypeDto and
CompanyDto contain id, igdbId and name. GameCompanyDto contains the
company reference plus developer, publisher, porting and supporting flags.
GameRelationDto contains relationType and a GameSummaryDto with id,
igdbId, name and coverImageId; they do not recursively include related games.
Missing collections are returned as empty arrays and optional metadata as null.
GameService maps lazy associations within a transaction; collection batching
avoids loading each game's collections individually on search pages.
Suggestions use GameSuggestion (id and name only), selected directly from the
database without loading game entities or their associations.

## Entries linked to games

entries.game_id is required, references games.id, and has its own index. The V1
column order starts with id, game_id, user_id. Entry no longer persists a title;
cover images remain catalog data. Entry has a lazy ManyToOne to Game with no
cascade, and Game does not need an inverse entries collection.
EntryDto is the HTTP response for entry detail, create, update and search
items. It contains personal entry fields plus EntryGameDto (id, name,
coverImageId). No complete GameDto or catalog collections are loaded for it.
POST /entries and PUT /entries/{id} accept SaveEntryRequest with required positive
gameId instead of title. The game must already exist (404 otherwise); the user is
resolved from the authenticated principal. Updates verify ownership before
resolving the game and never modify catalog data. Other editable fields retain
the existing replacement semantics.
Filters and sorts for the entry title now use game.name, including the default
sort. The frontend can construct image URLs from game.coverImageId.

A named entries_user_game_unique constraint enforces one entry per user and game.
Create and update flush inside the service and map only this constraint violation
to 409 Conflict. Other integrity errors are not treated as duplicates. No
preflight existence query is needed; the database also protects concurrent writes.

Entry updates must retain their existing gameId; a different gameId returns 400.
The editor displays the existing game name as read-only when editing.

# Entry search API

POST /api/entries/search
Content-Type: application/json

Public, read-only search by the unique username in the JSON body, independent of viewer identity. Unknown or disabled username returns 404. This exact read-only POST requires neither authentication nor CSRF; all write routes and protected single-entry reads retain their existing controls.

Example body:

    {
      "username": "alice",
      "pageNumber": 0,
      "pageSize": 100,
      "filters": [
        { "field": "title", "operator": "CONTAINS", "value": "Hollow" },
        { "field": "status", "operator": "EQUAL", "value": "UNPLAYED" }
      ],
      "sorts": [ { "field": "score", "direction": "DESC" } ]
    }

Response: { "items": [], "page": 0, "size": 100, "totalElements": 0, "totalPages": 0 }.

Defaults: pageNumber 0, pageSize 100, no filters, title ASC with ID ASC tie-breaker. Size must be 1–100. The username is required (nonblank, at most 40 characters); an object containing only username uses the defaults. An empty object is invalid; missing/null body is not. Null lists become empty lists; null list elements and null paging values are rejected. There is no need to expose a page-size selector in the UI.

Filters and sorts accept any direct basic field of Entry (for example title, status, score, notes, playtime, ownership or id); there is no per-field whitelist. Client relationship paths are not accepted. Metadata-driven operator checks:
- EQUAL and IN accept values compatible with the field type; enum and numeric strings are converted.
- CONTAINS requires a text field, matches literal substrings, and escapes SQL wildcards.
- GREATER_THAN and LESS_THAN require numeric/date fields (score for the current entry API).
- IS_NULL accepts an omitted or null value.
- IN accepts 1–100 values. All filters combine with AND.

Requested sorts accept ASC/DESC; duplicates are rejected and ID is appended for stable pagination. Null score ordering is PostgreSQL's default: last ascending, first descending. Text matching and ordering are case-insensitive. At most 20 client filters and 5 requested sorts. Invalid fields (including prezzo), types, operators, enum values or paging return 400. The field must exist as a direct basic attribute in the JPA metamodel; operators and values must be compatible with its Java type.

The service resolves the selected username and appends an internal user.id restriction before filtering, counting and pagination. Client ownership filters are rejected. A page beyond the end has empty items and retains the actual filtered total.

Status defaults to UNPLAYED for new entries and for explicit null status on create/update. The current database baseline defines the UNPLAYED default and NOT NULL status. Other optional fields can still be cleared.

The old GET /api/entries with query parameters and the username-in-path search route have been removed. POST /api/entries/search is implemented in EntryController and consumes EntrySearchCriteria extends SearchCriteria. CRUD and protected single-entry reads retain their Principal-based ownership checks.

Frontend migration is separate: send SearchCriteria, consume items/totalElements, reset page on filtering, debounce search, cancel stale requests and refetch after saves/deletes. Do not filter only the current page locally.

## IN values

FilterCriteria has Object value and List<Object> valueList. IN reads only valueList and ignores value. The list must contain 1–100 non-null elements, each compatible with the field's JPA type. Missing, empty, overlong or incompatible lists return 400. Arrays sent in value no longer satisfy IN. Other operators continue to use value and ignore valueList.

Example:

    { "field": "status", "operator": "IN", "valueList": ["PLAYING", "UNPLAYED"] }

Numeric values can be JSON numbers; enum values are strings. Every list element uses the same conversion and validation rules as a scalar value.

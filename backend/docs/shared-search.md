# Shared repository search

SearchRepository<T, ID> extends JpaRepository and exposes searchWithCriteria(criteria) plus validateCriteria(criteria). SearchRepositoryImpl extends SimpleJpaRepository and obtains the entity type and EntityManager from Spring Data's JpaEntityInformation. QuestlogApplication registers it through @EnableJpaRepositories(repositoryBaseClass = SearchRepositoryImpl.class).

EntryRepository and GameRepository extend SearchRepository. Other repositories can opt in by extending the same interface. No repository instance or entity class is passed by services. The package-private CriteriaSpecificationBuilder holds metamodel validation and query construction; CriteriaSearch, EntrySearch, SearchSchema and SearchField have been removed.

Usage inside a service:

    var criteria = new SearchCriteria(
        List.of(new FilterCriteria("title", FilterOperator.CONTAINS, "Hollow"),
                new FilterCriteria("user.id", FilterOperator.EQUAL, userId)),
        List.of(new SortCriteria("score", Sort.Direction.DESC)), 0, 100);
    var page = entryRepository.searchWithCriteria(criteria);

EntryService calls validateCriteria on the client criteria before resolving the selected user and appending the internal ownership filter. This method validates without executing SQL. searchWithCriteria validates the full criteria and then queries the database. Both methods are read-only transactions.

There is no field whitelist or allowedFields argument. The JPA metamodel determines field existence and Java type. Client validation accepts any direct basic attribute and rejects relationship/collection leaves and dotted paths. The server may append internal singular nested paths such as user.id before executing the search. No entity-specific field configuration is required.

Operators: EQUAL, CONTAINS, GREATER_THAN, LESS_THAN, IN and IS_NULL. Filters combine with AND. CONTAINS requires text and escapes SQL wildcard characters. GREATER_THAN/LESS_THAN require numeric or date fields. IN takes 1–100 non-null values; IS_NULL takes no value. Values are checked and enum/numeric strings converted; invalid input returns HTTP 400 before repository execution. Dates must already be typed values; string date parsing is not implemented. Text matching and sorting are case-insensitive.

Requested sorts preserve their order. The entity's ID attributes from JPA metadata are appended as unique tie-breakers. Null positioning is left to the database: PostgreSQL puts nulls last for ASC and first for DESC. No CASE expression or special null sorting remains.

Limits: page >= 0; size 1–100 (default 100); 20 client filters, plus one internal owner filter; 5 requested sorts. The default title sort is selected by EntryService when no sort was supplied. Another service can choose its own default or use the engine's ID ordering.

EntryService validates the incoming criteria through entryRepository.validateCriteria(criteria), resolves EntrySearchCriteria.username and appends user.id EQUAL userId using addFilter. Default sorting is appended with addSort. The same criteria object is passed to entryRepository.searchWithCriteria(criteria). All direct basic fields, including notes, playtime and ownership, can be filtered or sorted when their types support the selected operation. Public reads select the library from the username in the JSON body; write and item-read ownership checks continue to use Principal.

Entry status is non-null and defaults to UNPLAYED in the entity and database. The entity setter normalizes explicit null from create/update requests. The current database baseline defines the default and NOT NULL constraint; no query-time status coalescing is needed.

## IN values

FilterCriteria has Object value and List<Object> valueList. IN reads only valueList and ignores value. The list must contain 1–100 non-null elements, each compatible with the field's JPA type. Missing, empty, overlong or incompatible lists return 400. Arrays sent in value no longer satisfy IN. Other operators continue to use value and ignore valueList.

Example:

    { "field": "status", "operator": "IN", "valueList": ["PLAYING", "UNPLAYED"] }

Numeric values can be JSON numbers; enum values are strings. Every list element uses the same conversion and validation rules as a scalar value.

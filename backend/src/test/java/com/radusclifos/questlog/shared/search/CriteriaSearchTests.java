package com.radusclifos.questlog.shared.search;

import com.radusclifos.questlog.entry.Entry;
import com.radusclifos.questlog.entry.repository.EntryRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CriteriaSearchTests {
    private final CriteriaSpecificationBuilder<Entry> builder = SearchTestSupport.builder();
    static Stream<SearchCriteria> invalidCriteria() {
        return Stream.of(
            new SearchCriteria(List.of(), List.of(), -1, 100),
            new SearchCriteria(List.of(), List.of(), 0, 101),
            new SearchCriteria(List.of(), List.of(), 0, 0),
            filters(new FilterCriteria("prezzo", FilterOperator.EQUAL, 1)),
            filters(new FilterCriteria("title", FilterOperator.GREATER_THAN, "A")),
            filters(new FilterCriteria("status", FilterOperator.EQUAL, "UNKNOWN")),
            filters(new FilterCriteria("title", FilterOperator.EQUAL, 12)),
            filters(new FilterCriteria("score", FilterOperator.GREATER_THAN, "abc")),
            filters(new FilterCriteria("score", FilterOperator.EQUAL, Double.NaN)),
            filters(new FilterCriteria("score", FilterOperator.EQUAL, null)),
            filters(new FilterCriteria("score", FilterOperator.IS_NULL, "ignored")),
            filters(new FilterCriteria("score", FilterOperator.IN, null, List.of())),
            filters(new FilterCriteria("title", FilterOperator.IN, "not a list")),
            filters(new FilterCriteria("title", null, "test")),
            new SearchCriteria(List.of(), List.of(new SortCriteria("user", Sort.Direction.ASC))),
            new SearchCriteria(List.of(), List.of(new SortCriteria("unknown", Sort.Direction.ASC))),
            new SearchCriteria(List.of(), List.of(new SortCriteria("title", null))),
            new SearchCriteria(List.of(), List.of(new SortCriteria("title", Sort.Direction.ASC), new SortCriteria("title", Sort.Direction.DESC)))
        );
    }

    private static SearchCriteria filters(FilterCriteria filter) {
        return new SearchCriteria(List.of(filter), List.of());
    }

    @ParameterizedTest @MethodSource("invalidCriteria")
    void rejectsInvalidCriteriaBeforeRepositoryAccess(SearchCriteria criteria) {
        var repository = mock(EntryRepository.class);
        var exception = assertThrows(ResponseStatusException.class, () ->
            builder.build(criteria));
        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(repository);
    }

    @Test void acceptsConvertibleValuesAndMultipleSorts() {
        var criteria = new SearchCriteria(List.of(
            new FilterCriteria("status", FilterOperator.IN, null, List.of("UNPLAYED", "COMPLETED")),
            new FilterCriteria("score", FilterOperator.GREATER_THAN, "8.5")),
            List.of(new SortCriteria("score", Sort.Direction.DESC), new SortCriteria("title", Sort.Direction.ASC)));
        assertNotNull(builder.build(criteria));
    }

    @Test void rejectsNonexistentFieldsFromJpaMetadata() {
        var criteria = filters(new FilterCriteria("prezzo", FilterOperator.EQUAL, 10));
        var exception = assertThrows(ResponseStatusException.class, () -> builder.build(criteria));
        assertEquals(400, exception.getStatusCode().value());
    }

    @Test void acceptsAnyDirectBasicFieldWithoutConfiguration() {
        var criteria = new SearchCriteria(List.of(
            new FilterCriteria("notes", FilterOperator.CONTAINS, "adventure"),
            new FilterCriteria("playtime", FilterOperator.GREATER_THAN, "10"),
            new FilterCriteria("ownership", FilterOperator.EQUAL, "OWN")),
            List.of(new SortCriteria("playtime", Sort.Direction.DESC)));
        assertDoesNotThrow(() -> builder.validateClientCriteria(criteria));
    }

    @Test void acceptsClientRelationshipPaths() {
        var criteria = filters(new FilterCriteria("user.id", FilterOperator.EQUAL, 10L));
        assertDoesNotThrow(() -> builder.validateClientCriteria(criteria));
    }

    @Test void acceptsServerAddedUserFilter() {
        var criteria = filters(new FilterCriteria("user.id", FilterOperator.EQUAL, 10L));
        assertNotNull(builder.build(criteria));
    }
    @Test void inRequiresValueListAndIgnoresValue() {
        var valid = filters(new FilterCriteria("score", FilterOperator.IN, "ignored", List.of(7, "8.5")));
        assertDoesNotThrow(() -> builder.build(valid));
        for (java.util.List<Object> invalid : java.util.Arrays.<java.util.List<Object>>asList(
                null, List.of(), java.util.Arrays.asList(7, null), List.of("invalid"),
                java.util.Collections.nCopies(101, 7))) {
            var criteria = filters(new FilterCriteria("score", FilterOperator.IN, 7, invalid));
            assertThrows(ResponseStatusException.class, () -> builder.build(criteria));
        }
        // The old shape with a list in value is no longer accepted.
        assertThrows(ResponseStatusException.class, () -> builder.build(
                filters(new FilterCriteria("score", FilterOperator.IN, (Object) List.of(7, 8)))));
    }

    @Test void scalarOperatorsIgnoreValueList() {
        assertDoesNotThrow(() -> builder.build(filters(
                new FilterCriteria("score", FilterOperator.EQUAL, 7, List.of("ignored")))));
    }

}

package com.radusclifos.questlog.shared.search;

import com.radusclifos.questlog.entry.*;
import com.radusclifos.questlog.entry.repository.*;
import com.radusclifos.questlog.user.User;
import com.radusclifos.questlog.user.repository.UserRepository;
import com.radusclifos.questlog.game.Game;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import com.radusclifos.questlog.game.repository.GameRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @Transactional
class CriteriaSearchIntegrationTests {
    @Autowired EntryRepository entries;
    @Autowired UserRepository users;
    @Autowired EntityManager em;
    @Autowired GameRepository games;

    private Entry entry(User user, String title, Double score, EntryStatus status) {
        var entry = new Entry(title); entry.setUser(user); entry.setScore(score); entry.setStatus(status);
        return entries.saveAndFlush(entry);
    }

    private SearchCriteria forUser(SearchCriteria criteria, Long userId) {
        var filters = new ArrayList<>(criteria.getFilters());
        filters.add(new FilterCriteria("user.id", FilterOperator.EQUAL, userId));
        return new SearchCriteria(filters, criteria.getSorts(), criteria.getPageNumber(), criteria.getPageSize());
    }

    @Test void combinesFiltersConversionsScopesAndStableMultiSorts() {
        var user = users.save(new User("criteria-owner", "Owner", "hash"));
        var other = users.save(new User("criteria-other", "Other", "hash"));
        var first = entry(user, "Hollow Knight", 9.5, null);
        var second = entry(user, "hollow Knight", 9.5, EntryStatus.UNPLAYED);
        entry(user, "Hollow low", 5.0, EntryStatus.UNPLAYED);
        entry(user, "Hollow completed", 10.0, EntryStatus.COMPLETED);
        entry(other, "Hollow foreign", 9.5, null);
        var criteria = new SearchCriteria(List.of(
            new FilterCriteria("title", FilterOperator.CONTAINS, "HOLLOW"),
            new FilterCriteria("status", FilterOperator.IN, null, List.of("UNPLAYED")),
            new FilterCriteria("score", FilterOperator.GREATER_THAN, "8"),
            new FilterCriteria("score", FilterOperator.LESS_THAN, 10)),
            List.of(new SortCriteria("score", Sort.Direction.DESC), new SortCriteria("title", Sort.Direction.ASC)), 0, 1);
        criteria = forUser(criteria, user.getId());
        var page = entries.searchWithCriteria(criteria);
        assertEquals(2, page.getTotalElements()); assertEquals(first.getId(), page.getContent().getFirst().getId());
        var next = entries.searchWithCriteria(new SearchCriteria(criteria.getFilters(), criteria.getSorts(), 1, 1));
        assertEquals(second.getId(), next.getContent().getFirst().getId());
    }

    @Test void nullAndEqualityFiltersAndLiteralWildcardSearch() {
        var user = users.save(new User("criteria-null", "Null", "hash"));
        var special = entry(user, "100%_! Game", null, null);
        entry(user, "100abc Game", 8.0, EntryStatus.COMPLETED);
        var criteria = new SearchCriteria(List.of(
            new FilterCriteria("title", FilterOperator.CONTAINS, "%_!"),
            new FilterCriteria("score", FilterOperator.IS_NULL, null)), List.of());
        var page = entries.searchWithCriteria(criteria);
        assertEquals(List.of(special.getId()), page.map(Entry::getId).getContent());
        var equal = new SearchCriteria(List.of(new FilterCriteria("score", FilterOperator.EQUAL, "8.0")), List.of());
        assertEquals(1, entries.searchWithCriteria(forUser(equal, user.getId())).getTotalElements());
    }

    @Test void repositoryRejectsNonexistentFieldsAndDisallowedAccess() {
        var criteria = new SearchCriteria(List.of(new FilterCriteria("prezzo", FilterOperator.EQUAL, 10)), List.of());
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> entries.searchWithCriteria(criteria));
        assertEquals(400, error.getStatusCode().value());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> entries.validateCriteria(new SearchCriteria(List.of(new FilterCriteria("user.id", FilterOperator.EQUAL, 1L)), List.of())));
    }

    @Test void filtersAndSortsBasicFieldsWithoutAWhitelist() {
        var user = users.save(new User("basic-fields-owner", "Owner", "hash"));
        var first = entry(user, "First", 8.0, EntryStatus.PLAYING);
        first.setNotes("A memorable adventure"); first.setPlaytime(42); first.setOwnership(EntryOwnership.OWN);
        entries.saveAndFlush(first);
        var second = entry(user, "Second", 7.0, EntryStatus.UNPLAYED);
        second.setNotes("Another adventure"); second.setPlaytime(12); second.setOwnership(EntryOwnership.OWN);
        entries.saveAndFlush(second);
        var criteria = new SearchCriteria(List.of(
            new FilterCriteria("notes", FilterOperator.CONTAINS, "ADVENTURE"),
            new FilterCriteria("playtime", FilterOperator.GREATER_THAN, 10),
            new FilterCriteria("ownership", FilterOperator.EQUAL, "OWN")),
            List.of(new SortCriteria("playtime", Sort.Direction.DESC)));
        entries.validateCriteria(criteria);
        var result = entries.searchWithCriteria(forUser(criteria, user.getId()));
        assertEquals(List.of(first.getId(), second.getId()), result.map(Entry::getId).getContent());
    }

    @Test void sameEngineWorksWithAnotherEntityWithoutSchema() {
        var repository = games;
        var first = repository.save(new Game("Criteria Beta", null));
        repository.save(new Game("Criteria Alpha", null));
        var criteria = new SearchCriteria(List.of(new FilterCriteria("id", FilterOperator.EQUAL, first.getId().toString())),
            List.of(new SortCriteria("name", Sort.Direction.DESC)));
        var page = repository.searchWithCriteria(criteria);
        assertEquals(List.of(first.getId()), page.map(Game::getId).getContent());
    }
    @Test void inUsesValueListForNumbersAndEnums() {
        var user = users.save(new User("value-list-owner", "Owner", "hash"));
        var wanted = entry(user, "Matches list", 8.5, EntryStatus.PLAYING);
        entry(user, "Other score", 6.0, EntryStatus.PLAYING);
        entry(user, "Other status", 8.5, EntryStatus.COMPLETED);
        var criteria = new SearchCriteria(List.of(
            new FilterCriteria("score", FilterOperator.IN, "ignored", List.of(8.5, "9")),
            new FilterCriteria("status", FilterOperator.IN, "ignored", List.of("PLAYING"))), List.of());
        entries.validateCriteria(criteria);
        var result = entries.searchWithCriteria(forUser(criteria, user.getId()));
        assertEquals(List.of(wanted.getId()), result.map(Entry::getId).getContent());
    }

}

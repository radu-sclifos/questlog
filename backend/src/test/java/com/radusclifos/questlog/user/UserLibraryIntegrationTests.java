package com.radusclifos.questlog.user;

import com.radusclifos.questlog.entry.Entry;
import com.radusclifos.questlog.entry.dto.SaveEntryRequest;
import com.radusclifos.questlog.entry.repository.EntryRepository;
import com.radusclifos.questlog.entry.service.EntryService;
import com.radusclifos.questlog.user.dto.UpdateProfileRequest;
import com.radusclifos.questlog.user.repository.UserRepository;
import com.radusclifos.questlog.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserLibraryIntegrationTests {
    @Autowired UserRepository users;
    @Autowired EntryRepository entries;
    @Autowired EntryService library;
    @Autowired UserService profiles;

    private com.radusclifos.questlog.entry.dto.EntryPage search(String username, int page, int size,
            String text, com.radusclifos.questlog.entry.EntryStatus status, String sort) {
        var criteria = new com.radusclifos.questlog.entry.dto.EntrySearchCriteria(username);
        var filters = new java.util.ArrayList<com.radusclifos.questlog.shared.search.FilterCriteria>();
        if (text != null && !text.isBlank()) filters.add(new com.radusclifos.questlog.shared.search.FilterCriteria(
            "title", com.radusclifos.questlog.shared.search.FilterOperator.CONTAINS, text.strip()));
        if (status != null) filters.add(new com.radusclifos.questlog.shared.search.FilterCriteria(
            "status", com.radusclifos.questlog.shared.search.FilterOperator.EQUAL, status));
        criteria.setFilters(filters); criteria.setPageNumber(page); criteria.setPageSize(size);
        var order=sort.split(",");
        criteria.setSorts(java.util.List.of(new com.radusclifos.questlog.shared.search.SortCriteria(
            order[0], org.springframework.data.domain.Sort.Direction.fromString(order[1]))));
        return library.search(criteria);
    }

    @Test void usersHaveIsolatedWritableLibrariesAndCanReadFriends() {
        users.save(new User("integration-alice", "Alice", "hash"));
        users.save(new User("integration-bob", "Bob", "hash"));
        Principal alice=()->"integration-alice";
        Principal bob=()->"integration-bob";
        var created=library.create(alice,new SaveEntryRequest("Celeste",9.0,"Shared notes",null,null,null));
        assertEquals(1,search(alice.getName(),0,100,"",null,"title,asc").getItems().size());
        assertTrue(search(bob.getName(),0,100,"",null,"title,asc").getItems().isEmpty());
        assertEquals(created.getId(),search("integration-alice",0,100,"",null,"title,asc").getItems().getFirst().getId());
        assertThrows(ResponseStatusException.class,()->library.get(bob,created.getId()));
        assertThrows(ResponseStatusException.class,()->library.update(bob,created.getId(),new SaveEntryRequest("Changed",null,null,null,null,null)));
        assertThrows(ResponseStatusException.class,()->library.delete(bob,created.getId()));
        library.update(alice,created.getId(),new SaveEntryRequest("Updated",null,null,null,null,null));
        assertEquals("integration-alice",entries.findById(created.getId()).orElseThrow().getUser().getUsername());
        assertEquals("Updated",library.get(alice,created.getId()).getTitle());
        library.delete(alice,created.getId());
        assertTrue(search(alice.getName(),0,100,"",null,"title,asc").getItems().isEmpty());
    }

    @Test void paginationFiltersAndSortRunInDatabase() {
        var user=users.save(new User("pagination-user", "Pagination", "hash"));
        Principal principal=()->"pagination-user";
        for (int i=0;i<105;i++) {
            var entry=new Entry("Game %03d".formatted(i)); entry.setUser(user);
            entry.setStatus(i%2==0 ? com.radusclifos.questlog.entry.EntryStatus.COMPLETED : null);
            entry.setScore(i==0 ? null : (double)(i%11)); entries.save(entry);
        }
        var foreignUser=users.save(new User("pagination-other", "Other", "hash"));
        var foreign=new Entry("Game foreign"); foreign.setUser(foreignUser); entries.save(foreign); entries.flush();
        var first=search(principal.getName(),0,100,"",null,"title,asc");
        assertEquals(105,first.getTotalElements()); assertEquals(2,first.getTotalPages()); assertEquals(100,first.getItems().size());
        assertEquals("Game 000",first.getItems().getFirst().getTitle());
        assertEquals(5,search(principal.getName(),1,100,"",null,"title,asc").getItems().size());
        assertTrue(search(principal.getName(),2,100,"",null,"title,asc").getItems().isEmpty());
        assertEquals(52,search(principal.getName(),0,100," GAME ",com.radusclifos.questlog.entry.EntryStatus.UNPLAYED,"title,asc").getTotalElements());
        assertEquals(53,search(principal.getName(),0,100,"",com.radusclifos.questlog.entry.EntryStatus.COMPLETED,"title,asc").getTotalElements());
        assertEquals(0,search(principal.getName(),0,100,"%",null,"title,asc").getTotalElements());
        assertEquals("Game 104",search(principal.getName(),0,100,"",null,"title,desc").getItems().getFirst().getTitle());
        for (String sort : java.util.List.of("score,asc","score,desc")) {
            var nullPage=search(principal.getName(),sort.endsWith("asc") ? 1 : 0,100,"",null,sort);
            var nullEntry=sort.endsWith("asc") ? nullPage.getItems().getLast() : nullPage.getItems().getFirst();
            assertNull(nullEntry.getScore());
            assertEquals("Game 000",nullEntry.getTitle());
        }
    }

    @Test void profileUpdatesDoNotChangeIdentityAndDisabledUsersAreRejected() {
        var alice=users.save(new User("integration-alice", "Alice", "hash"));
        Principal principal=()->"integration-alice";
        assertEquals("New name",profiles.update(principal,new UpdateProfileRequest(" New name ")).getDisplayName());
        assertEquals("integration-alice",alice.getUsername());
        alice.disable(); users.flush();
        assertThrows(ResponseStatusException.class,()->profiles.current(principal));
        assertThrows(ResponseStatusException.class,()->profiles.find("integration-alice"));
    }

    @Test void migrationPlaceholderCannotAuthenticate() {
        var legacy=users.findByUsername("legacy-owner").orElseThrow();
        assertFalse(legacy.isEnabled());
        assertEquals("!",legacy.getPasswordHash());
    }
}

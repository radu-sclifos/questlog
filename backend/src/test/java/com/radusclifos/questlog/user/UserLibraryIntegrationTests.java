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

    @Test void usersHaveIsolatedWritableLibrariesAndCanReadFriends() {
        users.save(new User("integration-alice", "Alice", "hash"));
        users.save(new User("integration-bob", "Bob", "hash"));
        Principal alice=()->"integration-alice";
        Principal bob=()->"integration-bob";
        var created=library.create(alice,new SaveEntryRequest("Celeste",9.0,"Shared notes",null,null,null));
        assertEquals(1,library.mine(alice).size());
        assertTrue(library.mine(bob).isEmpty());
        assertEquals(created.getId(),library.forUser("integration-alice").getFirst().getId());
        assertThrows(ResponseStatusException.class,()->library.get(bob,created.getId()));
        assertThrows(ResponseStatusException.class,()->library.update(bob,created.getId(),new SaveEntryRequest("Changed",null,null,null,null,null)));
        assertThrows(ResponseStatusException.class,()->library.delete(bob,created.getId()));
        library.update(alice,created.getId(),new SaveEntryRequest("Updated",null,null,null,null,null));
        assertEquals("integration-alice",entries.findById(created.getId()).orElseThrow().getUser().getUsername());
        assertEquals("Updated",library.get(alice,created.getId()).getTitle());
        library.delete(alice,created.getId());
        assertTrue(library.mine(alice).isEmpty());
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

package com.radusclifos.questlog.entry;
import com.radusclifos.questlog.entry.service.EntryService;
import com.radusclifos.questlog.entry.repository.EntryRepository;
import com.radusclifos.questlog.entry.mapper.EntryMapper;
import com.radusclifos.questlog.entry.dto.SaveEntryRequest;
import com.radusclifos.questlog.user.User;
import com.radusclifos.questlog.user.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class EntryPermissionTests {
    private final EntryRepository repository=mock(EntryRepository.class);
    private final UserService users=mock(UserService.class);
    private final EntryMapper mapper=mock(EntryMapper.class);
    private final EntryService service=new EntryService(repository,mapper,users);
    private final Principal principal=()->"alice";
    @BeforeEach void setup() {
        com.radusclifos.questlog.shared.search.SearchTestSupport.configure(repository);
        var alice=new User("alice","Alice","hash"); ReflectionTestUtils.setField(alice,"id",10L);
        when(users.current(principal)).thenReturn(alice);
    }
    @Test void foreignEntriesCannotBeReadUpdatedOrDeleted() {
        when(repository.findByIdAndUserId(99L,10L)).thenReturn(Optional.empty());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.get(principal,99L)).getStatusCode().value());
        assertThrows(ResponseStatusException.class,()->service.update(principal,99L,new SaveEntryRequest("Title",null,null,null,null,null)));
        assertThrows(ResponseStatusException.class,()->service.delete(principal,99L));
        verify(repository,never()).save(any()); verify(repository,never()).delete(any(com.radusclifos.questlog.entry.Entry.class)); verifyNoInteractions(mapper);
    }
    @Test void creationAssignsAuthenticatedOwner() {
        var request=new SaveEntryRequest("Title",null,null,null,null,null);
        var entry=new Entry("Title"); when(mapper.toEntity(request)).thenReturn(entry);
        when(repository.save(entry)).thenReturn(entry);
        service.create(principal,request);
        assertEquals(10L,entry.getUser().getId());
    }
    @Test void friendsLibraryIsReadUsingFriendsId() {
        var bob=new User("bob","Bob","hash"); ReflectionTestUtils.setField(bob,"id",20L);
        when(users.find("bob")).thenReturn(bob);
        when(repository.searchWithCriteria(any(com.radusclifos.questlog.shared.search.SearchCriteria.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());
        service.search(new com.radusclifos.questlog.entry.dto.EntrySearchCriteria("bob"));
        verify(users).find("bob");
        verify(users, never()).current(any());
        verify(repository).searchWithCriteria(argThat(criteria -> criteria.getPageNumber() == 0 && criteria.getPageSize() == 100));
    }
}

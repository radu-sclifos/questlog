package com.radusclifos.questlog.entry;

import com.radusclifos.questlog.entry.controller.EntryController;
import com.radusclifos.questlog.entry.mapper.EntryMapper;
import com.radusclifos.questlog.entry.repository.EntryRepository;
import com.radusclifos.questlog.entry.service.EntryService;
import com.radusclifos.questlog.user.User;
import com.radusclifos.questlog.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EntrySearchControllerTests {
    EntryRepository repository;
    UserService users;
    MockMvc mvc;

    @BeforeEach void setup() {
        repository=mock(EntryRepository.class);
        com.radusclifos.questlog.shared.search.SearchTestSupport.configure(repository); users=mock(UserService.class);
        var user=new User("alice","Alice","hash"); ReflectionTestUtils.setField(user,"id",10L);
        when(users.find("alice")).thenReturn(user);
        when(repository.searchWithCriteria(any(com.radusclifos.questlog.shared.search.SearchCriteria.class)))
            .thenAnswer(invocation -> new PageImpl<Entry>(List.of(), PageRequest.of(((com.radusclifos.questlog.shared.search.SearchCriteria) invocation.getArgument(0)).getPageNumber(), ((com.radusclifos.questlog.shared.search.SearchCriteria) invocation.getArgument(0)).getPageSize()), 0));
        mvc=MockMvcBuilders.standaloneSetup(new EntryController(
            new EntryService(repository,Mappers.getMapper(EntryMapper.class),users))).build();
    }

    @Test void emptyCriteriaUsesDefaults() throws Exception {
        mvc.perform(post("/entries/search").contentType("application/json").content("{\"username\":\"alice\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(100)).andExpect(jsonPath("$.totalElements").value(0));
        verify(repository).searchWithCriteria(argThat(criteria -> criteria.getPageNumber() == 0 && criteria.getPageSize() == 100));
        verify(users,never()).current(any());
    }

    @Test void acceptsTypedCriteriaAndMultipleSorts() throws Exception {
        mvc.perform(post("/entries/search").contentType("application/json").content("""
            {"username":"alice","pageNumber":2,"pageSize":100,"filters":[
              {"field":"title","operator":"CONTAINS","value":"Hollow"},
              {"field":"status","operator":"IN","valueList":["UNPLAYED","COMPLETED"]},
              {"field":"score","operator":"GREATER_THAN","value":8}],
             "sorts":[{"field":"score","direction":"DESC"},{"field":"title","direction":"ASC"}]}
            """))
            .andExpect(status().isOk());
        verify(repository).searchWithCriteria(argThat(criteria -> criteria.getPageNumber() == 2 && criteria.getPageSize() == 100));
    }

    @ParameterizedTest
    @ValueSource(strings={
        "{\"pageNumber\":-1}", "{\"pageSize\":0}", "{\"pageSize\":101}",
        "{\"pageNumber\":null}", "{\"pageSize\":null}",
        "{\"filters\":[{\"field\":\"userId\",\"operator\":\"EQUAL\",\"value\":20}]}",
        "{\"filters\":[{\"field\":\"user.id\",\"operator\":\"EQUAL\",\"value\":20}]}",
        "{\"filters\":[{\"field\":\"prezzo\",\"operator\":\"CONTAINS\",\"value\":\"x\"}]}",
        "{\"filters\":[{\"field\":\"title\",\"operator\":\"UNKNOWN\",\"value\":\"x\"}]}",
        "{\"filters\":[{\"field\":\"score\",\"operator\":\"EQUAL\",\"value\":\"abc\"}]}",
        "{\"filters\":[{\"field\":\"status\",\"operator\":\"EQUAL\",\"value\":\"INVALID\"}]}",
        "{\"filters\":[{\"field\":\"title\",\"value\":\"x\"}]}",
        "{\"filters\":[null]}", "{\"sorts\":[null]}",
        "{\"sorts\":[{\"field\":\"userId\",\"direction\":\"ASC\"}]}",
        "{\"sorts\":[{\"field\":\"title\",\"direction\":\"INVALID\"}]}",
        "{\"sorts\":[{\"field\":\"title\",\"direction\":\"ASC\"},{\"field\":\"title\",\"direction\":\"DESC\"}]}"
    })
    void rejectsInvalidJsonCriteriaBeforeSearching(String body) throws Exception {
        mvc.perform(post("/entries/search").contentType("application/json").content(withUsername(body)))
            .andExpect(status().isBadRequest());
        verify(repository, never()).searchWithCriteria(any());
    }

    private String withUsername(String json) {
        return "{\"username\":\"alice\"," + json.substring(1);
    }

    @ParameterizedTest
    @ValueSource(strings={"{}", "{\"username\":null}", "{\"username\":\"\"}", "{\"username\":\"   \"}"})
    void requiresUsernameInBody(String body) throws Exception {
        mvc.perform(post("/entries/search").contentType("application/json").content(body))
            .andExpect(status().isBadRequest());
        verify(repository, never()).searchWithCriteria(any());
    }

    @Test void requiresJsonBody() throws Exception {
        mvc.perform(post("/entries/search").contentType("application/json"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/entries/search").contentType("application/json").content("null"))
            .andExpect(status().isBadRequest());
        verify(repository, never()).searchWithCriteria(any());
    }
}

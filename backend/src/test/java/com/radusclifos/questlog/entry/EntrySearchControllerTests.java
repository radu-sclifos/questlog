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
            new EntryService(repository,Mappers.getMapper(EntryMapper.class),users,mock(com.radusclifos.questlog.game.repository.GameRepository.class)))).build();
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
              {"field":"game.name","operator":"CONTAINS","value":"Hollow"},
              {"field":"status","operator":"IN","valueList":["UNPLAYED","COMPLETED"]},
              {"field":"score","operator":"GREATER_THAN","value":8}],
             "sorts":[{"field":"score","direction":"DESC"},{"field":"game.name","direction":"ASC"}]}
            """))
            .andExpect(status().isOk());
        verify(repository).searchWithCriteria(argThat(criteria -> criteria.getPageNumber() == 2 && criteria.getPageSize() == 100));
    }

    @Test void preservesNullsLastForScoreSort() throws Exception {
        mvc.perform(post("/entries/search").contentType("application/json").content("""
            {"username":"alice","sorts":[{"field":"score","direction":"DESC","nullsLast":true}]}
            """))
            .andExpect(status().isOk());
        verify(repository).searchWithCriteria(argThat(criteria ->
            criteria.findSort("score").orElseThrow().isNullsLast()));
    }

    @ParameterizedTest
    @ValueSource(strings={"ASC", "DESC"})
    void statusSortUsesServerOrderWithoutChangingPriorityOrDirection(String direction) throws Exception {
        mvc.perform(post("/entries/search").contentType("application/json").content("""
            {"username":"alice","sorts":[
              {"field":"score","direction":"DESC"},
              {"field":"status","direction":"%s","customOrder":["UNPLAYED","PLAYING"]},
              {"field":"game.name","direction":"ASC"}]}
            """.formatted(direction)))
            .andExpect(status().isOk());
        verify(repository).searchWithCriteria(argThat(criteria -> {
            var sorts = criteria.getSorts();
            return sorts.size() == 3
                && sorts.get(0).getField().equals("score")
                && sorts.get(1).getField().equals("status")
                && sorts.get(1).getDirection() == Sort.Direction.valueOf(direction)
                && sorts.get(1).getCustomOrder().equals(List.of(
                    "PLAYING", "ON_HOLD", "COMPLETED", "BEATEN", "ENDLESS", "DROPPED", "UNPLAYED"))
                && sorts.get(2).getField().equals("game.name");
        }));
    }

    @ParameterizedTest
    @ValueSource(strings={
        "{\"pageNumber\":-1}", "{\"pageSize\":0}", "{\"pageSize\":101}",
        "{\"pageNumber\":null}", "{\"pageSize\":null}",
        "{\"filters\":[{\"field\":\"userId\",\"operator\":\"EQUAL\",\"value\":20}]}",
        "{\"filters\":[{\"field\":\"user.passwordHash\",\"operator\":\"CONTAINS\",\"value\":\"hash\"}]}",
        "{\"filters\":[{\"field\":\"prezzo\",\"operator\":\"CONTAINS\",\"value\":\"x\"}]}",
        "{\"filters\":[{\"field\":\"game.name\",\"operator\":\"UNKNOWN\",\"value\":\"x\"}]}",
        "{\"filters\":[{\"field\":\"score\",\"operator\":\"EQUAL\",\"value\":\"abc\"}]}",
        "{\"filters\":[{\"field\":\"status\",\"operator\":\"EQUAL\",\"value\":\"INVALID\"}]}",
        "{\"filters\":[{\"field\":\"game.name\",\"value\":\"x\"}]}",
        "{\"filters\":[null]}", "{\"sorts\":[null]}",
        "{\"sorts\":[{\"field\":\"userId\",\"direction\":\"ASC\"}]}",
        "{\"sorts\":[{\"field\":\"game.name\",\"direction\":\"INVALID\"}]}",
        "{\"sorts\":[{\"field\":\"game.name\",\"direction\":\"ASC\"},{\"field\":\"game.name\",\"direction\":\"DESC\"}]}"
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
    @ValueSource(strings={
        "{\"field\":\"status\",\"customOrder\":[]}",
        "{\"field\":\"status\",\"customOrder\":[\"INVALID\"]}",
        "{\"field\":\"status\",\"customOrder\":[null]}",
        "{\"field\":\"status\",\"customOrder\":[\"PLAYING\",\"PLAYING\"]}",
        "{\"field\":\"game.name\",\"customOrder\":[\"PLAYING\"]}"
    })
    void rejectsInvalidCustomOrder(String sort) throws Exception {
        var body = "{\"username\":\"alice\",\"sorts\":[" + sort.substring(0, sort.length() - 1)
                + ",\"direction\":\"ASC\"}]}";
        mvc.perform(post("/entries/search").contentType("application/json").content(body))
            .andExpect(status().isBadRequest());
        verify(repository, never()).searchWithCriteria(any());
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

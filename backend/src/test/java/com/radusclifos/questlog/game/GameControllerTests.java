package com.radusclifos.questlog.game;

import com.radusclifos.questlog.game.controller.GameController;
import com.radusclifos.questlog.game.repository.GameRepository;
import com.radusclifos.questlog.game.dto.GameSuggestion;
import com.radusclifos.questlog.game.mapper.GameMapper;
import com.radusclifos.questlog.game.service.GameService;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.radusclifos.questlog.shared.search.SearchCriteria;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GameControllerTests {
    private GameRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository = mock(GameRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(new GameController(new GameService(repository, Mappers.getMapper(GameMapper.class)))).build();
    }

    @Test
    void searchesWithCriteriaAndReturnsPageMetadata() throws Exception {
        var game = new Game("Hollow Knight", "co_example");
        ReflectionTestUtils.setField(game, "id", 1L);
        when(repository.searchWithCriteria(any())).thenReturn(
                new PageImpl<>(List.of(game), PageRequest.of(1, 5), 6));
        mvc.perform(post("/games/search").contentType(MediaType.APPLICATION_JSON).content("""
                {"filters":[{"field":"genres.id","operator":"EQUAL","value":3}],
                 "sorts":[{"field":"name","direction":"ASC"}],"pageNumber":1,"pageSize":5}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Hollow Knight"))
                .andExpect(jsonPath("$.items[0].coverImageId").value("co_example"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));
        var order = inOrder(repository);
        order.verify(repository).validateCriteria(any(SearchCriteria.class));
        order.verify(repository).searchWithCriteria(argThat(criteria ->
                criteria.getFilters().getFirst().getField().equals("genres.id")
                        && criteria.getPageNumber() == 1 && criteria.getPageSize() == 5));
    }

    @Test
    void rejectsInvalidSearchBeforeQuerying() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown search field"))
                .when(repository).validateCriteria(any());
        mvc.perform(post("/games/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filters\":[{\"field\":\"unknown\",\"operator\":\"EQUAL\",\"value\":1}]}"))
                .andExpect(status().isBadRequest());
        verify(repository, never()).searchWithCriteria(any());
        mvc.perform(post("/games/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageSize\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void suggestsGamesByNameWithABoundedResultList() throws Exception {
        var game = new Game("Hollow Knight", "co_example");
        ReflectionTestUtils.setField(game, "id", 1L);
        when(repository.findByNameContainingIgnoreCase(eq("Holl"), any())).thenReturn(List.of(new GameSuggestion(1L, "Hollow Knight")));
        mvc.perform(get("/games/suggestions").param("name", " Holl "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Hollow Knight"))
                .andExpect(jsonPath("$[0].coverImageId").doesNotExist())
                .andExpect(jsonPath("$[0].title").doesNotExist());
        verify(repository).findByNameContainingIgnoreCase(eq("Holl"), argThat(page ->
                page.getPageSize() == 10 && page.getSort().getOrderFor("name").isIgnoreCase()));
        mvc.perform(get("/games/suggestions").param("name", "Missing").param("limit", "5"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(repository).findByNameContainingIgnoreCase(eq("Missing"), argThat(page -> page.getPageSize() == 5));
    }

    @Test
    void rejectsMissingBlankNamesAndInvalidLimits() throws Exception {
        mvc.perform(get("/games/suggestions")).andExpect(status().isBadRequest());
        for (var name : List.of("", "   ")) {
            mvc.perform(get("/games/suggestions").param("name", name)).andExpect(status().isBadRequest());
        }
        for (var limit : List.of("0", "invalid")) {
            mvc.perform(get("/games/suggestions").param("name", "Holl").param("limit", limit))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(repository);
    }

    @Test
    void readsGamesUsingNumericIds() throws Exception {
        Game game = new Game("Hollow Knight", "co_example");
        ReflectionTestUtils.setField(game, "id", 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(game));

        mvc.perform(get("/games/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        verify(repository).findById(1L);
    }

    @Test
    void rejectsUuidPaths() throws Exception {
        mvc.perform(get("/games/7ac6763d-f4d0-42ab-8abd-eab3a441756a"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    @Test
    void missingGameReturnsNotFound() throws Exception {
        mvc.perform(get("/games/1")).andExpect(status().isNotFound());
    }
}

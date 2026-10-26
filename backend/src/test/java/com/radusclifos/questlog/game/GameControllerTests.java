package com.radusclifos.questlog.game;

import com.radusclifos.questlog.game.controller.GameController;
import com.radusclifos.questlog.game.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
        mvc = MockMvcBuilders.standaloneSetup(new GameController(repository)).build();
    }

    @Test
    void createsGameWithNumericId() throws Exception {
        when(repository.save(any(Game.class))).thenAnswer(invocation -> {
            Game game = invocation.getArgument(0);
            ReflectionTestUtils.setField(game, "id", 1L);
            return game;
        });

        mvc.perform(post("/games").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hollow Knight\",\"coverUrl\":null}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void readsGamesUsingNumericIds() throws Exception {
        Game game = new Game("Hollow Knight", null);
        ReflectionTestUtils.setField(game, "id", 1L);
        when(repository.findAll()).thenReturn(List.of(game));
        when(repository.findById(1L)).thenReturn(Optional.of(game));

        mvc.perform(get("/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
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

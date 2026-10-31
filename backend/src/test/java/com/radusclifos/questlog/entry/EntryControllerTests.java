package com.radusclifos.questlog.entry;

import com.radusclifos.questlog.entry.controller.EntryController;
import com.radusclifos.questlog.entry.mapper.EntryMapper;
import com.radusclifos.questlog.entry.repository.EntryRepository;
import com.radusclifos.questlog.entry.service.EntryService;
import com.radusclifos.questlog.game.Game;
import com.radusclifos.questlog.game.repository.GameRepository;
import com.radusclifos.questlog.user.User;
import com.radusclifos.questlog.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EntryControllerTests {
    EntryRepository entries;
    GameRepository games;
    MockMvc mvc;
    Game game;

    @BeforeEach void setup() {
        entries = mock(EntryRepository.class);
        games = mock(GameRepository.class);
        var users = mock(UserService.class);
        var user = new User("tester", "Tester", "hash");
        ReflectionTestUtils.setField(user, "id", 10L);
        when(users.current(any())).thenReturn(user);
        game = new Game("Hollow Knight", "co_example");
        ReflectionTestUtils.setField(game, "id", 2L);
        when(games.findById(2L)).thenReturn(Optional.of(game));
        when(entries.save(any())).thenAnswer(invocation -> {
            Entry entry = invocation.getArgument(0);
            ReflectionTestUtils.setField(entry, "id", 1L);
            return entry;
        });
        mvc = MockMvcBuilders.standaloneSetup(new EntryController(
                new EntryService(entries, Mappers.getMapper(EntryMapper.class), users, games))).build();
    }

    @Test void createsEntryUsingAnExistingGame() throws Exception {
        mvc.perform(post("/entries").contentType("application/json").content("{\"gameId\":2,\"score\":9}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.game.id").value(2))
                .andExpect(jsonPath("$.game.name").value("Hollow Knight"))
                .andExpect(jsonPath("$.game.coverImageId").value("co_example"))
                .andExpect(jsonPath("$.game.genres").doesNotExist())
                .andExpect(jsonPath("$.title").doesNotExist());
        verify(games, never()).save(any());
    }

    @Test void updatesAnOwnedEntryAndReadsItsGame() throws Exception {
        var entry = new Entry(game);
        when(entries.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(entry));
        mvc.perform(put("/entries/1").contentType("application/json").content("{\"gameId\":2,\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/entries/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.game.name").value("Hollow Knight"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"gameId\":null}", "{\"gameId\":0}", "{\"gameId\":-1}",
            "{\"title\":\"Manual game\"}", "{\"gameId\":2,\"score\":11}", "{\"gameId\":2,\"status\":\"UNKNOWN\"}"})
    void rejectsInvalidRequests(String body) throws Exception {
        mvc.perform(post("/entries").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        verify(entries, never()).save(any());
    }

    @Test void rejectsMissingGameAndForeignEntries() throws Exception {
        mvc.perform(post("/entries").contentType("application/json").content("{\"gameId\":999}"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/entries/99").contentType("application/json").content("{\"gameId\":2}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/entries/99")).andExpect(status().isNotFound());
        verify(entries, never()).save(any());
    }
}

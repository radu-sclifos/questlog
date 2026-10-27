package com.radusclifos.questlog.entry;

import com.radusclifos.questlog.entry.controller.EntryController;
import com.radusclifos.questlog.entry.mapper.EntryMapper;
import com.radusclifos.questlog.entry.repository.EntryRepository;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EntryControllerTests {

    private EntryRepository repository;
    private MockMvc mvc;
    private final Long id = 1L;

    @BeforeEach
    void setUp() {
        repository = mock(EntryRepository.class);
        com.radusclifos.questlog.shared.search.SearchTestSupport.configure(repository);
        var users = mock(com.radusclifos.questlog.user.service.UserService.class);
        var user = new com.radusclifos.questlog.user.User("tester", "Tester", "hash");
        ReflectionTestUtils.setField(user, "id", 10L);
        when(users.current(any())).thenReturn(user);
        mvc = MockMvcBuilders.standaloneSetup(
                new EntryController(new com.radusclifos.questlog.entry.service.EntryService(repository, Mappers.getMapper(EntryMapper.class), users))).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/api"})
    void createsEntryWithManualTitle(String contextPath) throws Exception {
        when(repository.save(any(Entry.class))).thenAnswer(invocation -> {
            Entry entry = invocation.getArgument(0);
            ReflectionTestUtils.setField(entry, "id", id);
            return entry;
        });

        mvc.perform(post(contextPath + "/entries").contextPath(contextPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  Hollow Knight  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.intValue()))
                .andExpect(jsonPath("$.title").value("Hollow Knight"));
    }

    @Test
    void readsEntries() throws Exception {
        Entry entry = existingEntry();
        when(repository.searchWithCriteria(any(com.radusclifos.questlog.shared.search.SearchCriteria.class)))
                .thenReturn(new PageImpl<>(List.of(entry), PageRequest.of(0, 100), 1));
        when(repository.findByIdAndUserId(id, 10L)).thenReturn(Optional.of(entry));

        mvc.perform(get("/entries/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hollow Knight"))
                .andExpect(jsonPath("$.score").value(9.0))
                .andExpect(jsonPath("$.notes").value("A great game"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void updatesAllEditableFields() throws Exception {
        when(repository.findByIdAndUserId(id, 10L)).thenReturn(Optional.of(existingEntry()));
        when(repository.save(any(Entry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mvc.perform(put("/entries/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Celeste\",\"score\":8.5,\"notes\":\"Updated notes\",\"status\":\"ON_HOLD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()))
                .andExpect(jsonPath("$.title").value("Celeste"))
                .andExpect(jsonPath("$.score").value(8.5))
                .andExpect(jsonPath("$.notes").value("Updated notes"))
                .andExpect(jsonPath("$.status").value("ON_HOLD"));
        verify(repository).save(any(Entry.class));
    }

    @Test
    void deletesEntry() throws Exception {
        Entry entry = existingEntry();
        when(repository.findByIdAndUserId(id, 10L)).thenReturn(Optional.of(entry));

        mvc.perform(delete("/entries/{id}", id)).andExpect(status().isNoContent());
        verify(repository).delete(entry);
    }

    @Test
    void missingEntriesReturnNotFound() throws Exception {
        mvc.perform(get("/entries/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(put("/entries/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Celeste\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/entries/{id}", id)).andExpect(status().isNotFound());
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any(com.radusclifos.questlog.entry.Entry.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"title\":null}", "{\"title\":\"\"}", "{\"title\":\"   \"}"})
    void rejectsMissingAndBlankTitles(String body) throws Exception {
        assertInvalidTitle(body);
    }

    @Test
    void rejectsOverlongTitles() throws Exception {
        assertInvalidTitle("{\"title\":\"" + "a".repeat(201) + "\"}");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"title\":\"Game\",\"score\":-0.1}",
            "{\"title\":\"Game\",\"score\":10.1}",
            "{\"title\":\"Game\",\"status\":\"UNKNOWN\"}"
    })
    void rejectsInvalidDetails(String body) throws Exception {
        assertInvalidTitle(body);
    }

    @Test
    void rejectsOverlongNotes() throws Exception {
        assertInvalidTitle("{\"title\":\"Game\",\"notes\":\"" + "a".repeat(2001) + "\"}");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 10.0})
    void createsEntryWithDetailsAtValidationLimits(double score) throws Exception {
        when(repository.save(any(Entry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String notes = "a".repeat(2000);

        mvc.perform(post("/entries").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Game\",\"score\":" + score
                                + ",\"notes\":\"" + notes + "\",\"status\":\"COMPLETED\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(score))
                .andExpect(jsonPath("$.notes").value(notes))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void clearsOptionalDetailsWhenOmittedOnUpdate() throws Exception {
        Entry entry = existingEntry();
        when(repository.findByIdAndUserId(id, 10L)).thenReturn(Optional.of(entry));
        when(repository.save(any(Entry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mvc.perform(put("/entries/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Celeste\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").doesNotExist())
                .andExpect(jsonPath("$.notes").doesNotExist())
                .andExpect(jsonPath("$.status").value("UNPLAYED"));
    }

    private void assertInvalidTitle(String body) throws Exception {
        mvc.perform(post("/entries").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/entries/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verify(repository, never()).searchWithCriteria(any());
    }

    private Entry existingEntry() {
        Entry entry = new Entry("Hollow Knight");
        ReflectionTestUtils.setField(entry, "id", id);
        entry.setScore(9.0);
        entry.setNotes("A great game");
        entry.setStatus(EntryStatus.COMPLETED);
        return entry;
    }
}

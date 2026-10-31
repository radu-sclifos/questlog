package com.radusclifos.questlog.entry;

import com.radusclifos.questlog.entry.repository.EntryRepository;
import com.radusclifos.questlog.user.User;
import com.radusclifos.questlog.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @Transactional
class EntryJsonSearchIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired EntryRepository entries;
    @Autowired jakarta.persistence.EntityManager em;

    @Test void nullScoresComeLastInBothDirectionsBeforePaging() throws Exception {
        var user = users.save(new User("null-score-sort", "Scores", "hash"));
        for (int i = 0; i < 4; i++) {
            var entry = new Entry(new com.radusclifos.questlog.game.Game("Game " + i, null));
            entry.setUser(user);
            entry.setScore(i < 2 ? (double) (i + 1) : null);
            saveEntry(entry);
        }
        entries.flush();
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for (var direction : java.util.List.of("ASC", "DESC")) {
            var request = """
                {"username":"null-score-sort","pageSize":2,"pageNumber":%d,"sorts":[
                  {"field":"score","direction":"%s","nullsLast":true},
                  {"field":"game.name","direction":"ASC"}]}
                """;
            mvc.perform(post("/entries/search").contentType("application/json")
                    .content(request.formatted(0, direction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.items[0].game.name").value(direction.equals("ASC") ? "Game 0" : "Game 1"))
                .andExpect(jsonPath("$.items[1].game.name").value(direction.equals("ASC") ? "Game 1" : "Game 0"));
            mvc.perform(post("/entries/search").contentType("application/json")
                    .content(request.formatted(1, direction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].game.name").value("Game 2"))
                .andExpect(jsonPath("$.items[1].game.name").value("Game 3"));
        }
    }

    @Test void customStatusOrderIsAppliedBeforePaging() throws Exception {
        var user = users.save(new User("custom-sort", "Custom", "hash"));
        for (var entryStatus : EntryStatus.values()) {
            var entry = new Entry(new com.radusclifos.questlog.game.Game(entryStatus.name(), null));
            entry.setUser(user);
            entry.setStatus(entryStatus);
            saveEntry(entry);
        }
        entries.flush();
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var request = """
            {"username":"custom-sort","pageSize":2,"pageNumber":%d,
             "sorts":[{"field":"status","direction":"%s",
             "customOrder":["PLAYING","ON_HOLD","COMPLETED","BEATEN","ENDLESS","DROPPED","UNPLAYED"]}]}
            """;
        mvc.perform(post("/entries/search").contentType("application/json").content(request.formatted(0, "ASC")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(7))
            .andExpect(jsonPath("$.items[0].status").value("PLAYING"))
            .andExpect(jsonPath("$.items[1].status").value("ON_HOLD"));
        mvc.perform(post("/entries/search").contentType("application/json").content(request.formatted(1, "ASC")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].status").value("COMPLETED"))
            .andExpect(jsonPath("$.items[1].status").value("BEATEN"));
        mvc.perform(post("/entries/search").contentType("application/json").content(request.formatted(0, "DESC")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].status").value("UNPLAYED"))
            .andExpect(jsonPath("$.items[1].status").value("DROPPED"));
        mvc.perform(post("/entries/search").contentType("application/json").content("""
            {"username":"custom-sort","sorts":[
              {"field":"status","direction":"ASC","customOrder":["PLAYING"]},
              {"field":"game.name","direction":"ASC"}]}
            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].status").value("PLAYING"))
            .andExpect(jsonPath("$.items[1].status").value("ON_HOLD"));
    }

    @Test void selectedLibraryIsFilteredAndPagedBeforeReturningResults() throws Exception {
        var alice=users.save(new User("json-alice","Alice","hash"));
        var bob=users.save(new User("json-bob","Bob","hash"));
        for(int i=0;i<105;i++) {
            var entry=new Entry(new com.radusclifos.questlog.game.Game("Hollow %03d".formatted(i), null)); entry.setUser(alice);
            entry.setScore(i==0 ? null : 9.0);
            entry.setNotes(i==0 ? "A memorable adventure" : null);
            entry.setPlaytime(i==0 ? 42 : 2); entry.setOwnership(EntryOwnership.OWN); saveEntry(entry);
        }
        var foreign=new Entry(new com.radusclifos.questlog.game.Game("Hollow foreign", null)); foreign.setUser(bob); saveEntry(foreign);
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(post("/entries/search").contentType("application/json").content("""
            {"username":"json-alice","pageNumber":1,"filters":[{"field":"game.name","operator":"CONTAINS","value":"HOLLOW"},
                {"field":"status","operator":"EQUAL","value":"UNPLAYED"}],
             "sorts":[{"field":"score","direction":"DESC"}]}
            """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(105))
            .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.items.length()").value(5))
            .andExpect(jsonPath("$.items[4].game.name").value("Hollow 104"));
        mvc.perform(post("/entries/search").contentType("application/json").content("""
            {"username":"json-alice","sorts":[{"field":"score","direction":"DESC"}]}
            """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].game.name").value("Hollow 000"));
        mvc.perform(post("/entries/search").contentType("application/json").content("""
            {"username":"json-alice","filters":[
              {"field":"notes","operator":"CONTAINS","value":"ADVENTURE"},
              {"field":"playtime","operator":"GREATER_THAN","value":10},
              {"field":"ownership","operator":"EQUAL","value":"OWN"}],
             "sorts":[{"field":"playtime","direction":"DESC"}]}
            """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.items[0].game.name").value("Hollow 000"));
        mvc.perform(post("/entries/search").contentType("application/json").content("{\"username\":\"json-bob\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.items[0].game.name").value("Hollow foreign"));
        mvc.perform(post("/entries/search").contentType("application/json").content("""
            {"username":"json-alice","filters":[{"field":"userId","operator":"EQUAL","value":123}]}
            """))
            .andExpect(status().isBadRequest());
    }
    private Entry saveEntry(Entry entry) {
        em.persist(entry.getGame());
        return entries.save(entry);
    }
}

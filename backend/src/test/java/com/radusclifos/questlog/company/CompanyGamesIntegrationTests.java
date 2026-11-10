package com.radusclifos.questlog.company;

import com.radusclifos.questlog.company.controller.CompanyController;
import com.radusclifos.questlog.game.Game;
import com.radusclifos.questlog.game.GameCompany;
import com.radusclifos.questlog.support.PostgresTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@PostgresTest
@SpringBootTest
@Transactional
class CompanyGamesIntegrationTests {

    @Autowired
    EntityManager em;

    @Autowired
    CompanyController controller;

    @Test
    void returnsAvailableRolesAndListsEachGameOncePerMatchingRole() throws Exception {
        var company = new Company("Example Studio");
        var other = new Company("Other Studio");
        var empty = new Company("Empty Studio");
        em.persist(company);
        em.persist(other);
        em.persist(empty);

        var both = involvement(company, "Zelda");
        both.setDeveloper(true);
        both.setPublisher(true);
        var developed = involvement(company, "alpha");
        developed.setDeveloper(true);
        var ported = involvement(company, "Port");
        ported.setPorting(true);
        var unrelated = involvement(other, "Unrelated");
        unrelated.setDeveloper(true);
        unrelated.setSupporting(true);
        em.flush();

        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        var companyUrl = "/companies/" + company.getId();
        var gamesUrl = companyUrl + "/games";
        mvc.perform(get(companyUrl)).andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", contains("developer", "publisher", "porting")));
        mvc.perform(get(gamesUrl).param("role", "developer")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name", contains("alpha", "Zelda")))
                .andExpect(jsonPath("$.size").value(24));
        mvc.perform(get(gamesUrl).param("role", "developer").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name", contains("alpha")))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get(gamesUrl).param("role", "developer").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name", contains("Zelda")))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get(gamesUrl).param("role", "publisher")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name", contains("Zelda")));
        mvc.perform(get(gamesUrl).param("role", "porting")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name", contains("Port")));
        mvc.perform(get(gamesUrl).param("role", "supporting")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalPages").value(0));
        mvc.perform(get("/companies/{id}/games", other.getId()).param("role", "supporting"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[*].name", contains("Unrelated")));
        mvc.perform(get("/companies/{id}", empty.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isEmpty());
        mvc.perform(get(gamesUrl)).andExpect(status().isBadRequest());
        mvc.perform(get(gamesUrl).param("role", "unknown")).andExpect(status().isBadRequest());
        mvc.perform(get(gamesUrl).param("role", "developer").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(gamesUrl).param("role", "developer").param("size", "101"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/companies/{id}/games", Long.MAX_VALUE).param("role", "developer"))
                .andExpect(status().isNotFound());
    }

    private GameCompany involvement(Company company, String gameName) {
        var game = new Game(gameName, null);
        em.persist(game);
        var involvement = new GameCompany(game, company);
        em.persist(involvement);
        return involvement;
    }
}

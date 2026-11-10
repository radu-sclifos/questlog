package com.radusclifos.questlog.company;

import com.radusclifos.questlog.company.controller.CompanyController;
import com.radusclifos.questlog.company.repository.CompanyRepository;
import com.radusclifos.questlog.company.service.CompanyService;
import com.radusclifos.questlog.game.repository.GameCompanyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CompanyControllerTests {

    @Test
    void returnsCompanyDetailsWithEnglishCountryName() throws Exception {
        var companies = mock(CompanyRepository.class);
        var company = new Company("Example Studio");
        company.setCountryCode(380);
        company.setLogoImageId("studio_logo");
        when(companies.findById(1L)).thenReturn(Optional.of(company));
        var mvc = mvcFor(companies);

        mvc.perform(get("/companies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Example Studio"))
                .andExpect(jsonPath("$.country").value("Italy"))
                .andExpect(jsonPath("$.foundedDate").value(nullValue()))
                .andExpect(jsonPath("$.logoImageId").value("studio_logo"))
                .andExpect(jsonPath("$.countryCode").doesNotHaveJsonPath());
        mvc.perform(get("/companies/2")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({"YYYYMMMMDD, 23/05/1996", "YYYYMMMM, 05/1996", "YYYY, 1996", "2, 1996", ", 1996", "TBD,"})
    void returnsFoundingDateAsDisplayText(String format, String expected) throws Exception {
        var companies = mock(CompanyRepository.class);
        var company = new Company("Example Studio");
        company.setFoundedDate(LocalDate.of(1996, 5, 23));
        company.setFoundedDateFormat(format);
        when(companies.findById(1L)).thenReturn(Optional.of(company));
        var mvc = mvcFor(companies);

        mvc.perform(get("/companies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foundedDate").value(expected))
                .andExpect(jsonPath("$.foundedDateFormat").doesNotHaveJsonPath());
    }

    private MockMvc mvcFor(CompanyRepository companies) {
        var gameCompanies = mock(GameCompanyRepository.class);
        when(gameCompanies.findRoleAvailability(1L)).thenReturn(mock(GameCompanyRepository.RoleAvailability.class));
        return MockMvcBuilders.standaloneSetup(new CompanyController(new CompanyService(companies, gameCompanies))).build();
    }
}

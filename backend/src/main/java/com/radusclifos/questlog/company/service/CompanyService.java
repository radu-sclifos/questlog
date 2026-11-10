package com.radusclifos.questlog.company.service;

import com.radusclifos.questlog.company.CompanyRole;
import com.radusclifos.questlog.company.dto.CompanyDetailDto;
import com.radusclifos.questlog.company.dto.CompanyGamePage;
import com.radusclifos.questlog.company.repository.CompanyRepository;
import com.radusclifos.questlog.game.repository.GameCompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    @Autowired
    private final CompanyRepository companies;
    private final GameCompanyRepository gameCompanies;

    private static final Properties COUNTRY_NAMES = loadCountryNames();
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT);
    private static final DateTimeFormatter MONTH_AND_YEAR = DateTimeFormatter.ofPattern("MM/uuuu", Locale.ROOT);
    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("uuuu", Locale.ROOT);

    public CompanyDetailDto findById(Long id) {
        var company = companies.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
        var countryCode = company.getCountryCode();
        var country = countryCode == null ? null : COUNTRY_NAMES.getProperty(countryCode.toString());
        return new CompanyDetailDto(company.getId(), company.getIgdbId(), company.getName(),
                country, formatFoundedDate(company.getFoundedDate(), company.getFoundedDateFormat()),
                company.getLogoImageId(), availableRoles(id));
    }

    public CompanyGamePage findGames(Long id, String role, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
        }
        CompanyRole companyRole;

        try {
            companyRole = CompanyRole.valueOf(role.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown company role");
        }
        if (!companies.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found");
        }

        return new CompanyGamePage(gameCompanies.findGameSummariesByCompanyAndRole(
                id, companyRole.value(), PageRequest.of(page, size)));
    }

    private List<CompanyRole> availableRoles(Long companyId) {
        var availability = gameCompanies.findRoleAvailability(companyId);

        var roles = EnumSet.noneOf(CompanyRole.class);
        if (availability.getDeveloper()) {
            roles.add(CompanyRole.DEVELOPER);
        }
        if (availability.getPublisher()) {
            roles.add(CompanyRole.PUBLISHER);
        }
        if (availability.getPorting()) {
            roles.add(CompanyRole.PORTING);
        }
        if (availability.getSupporting()) {
            roles.add(CompanyRole.SUPPORTING);
        }

        return List.copyOf(roles);
    }

    private static String formatFoundedDate(LocalDate date, String format) {
        if (date == null) return null;
        if (format == null) return YEAR.format(date);

        return switch (format.strip().toUpperCase(Locale.ROOT).replaceAll("[\\s/.-]", "")) {
            case "YYYYMMMMDD", "YYYYMMDD", "DDMMMMYYYY", "DDMMYYYY" -> FULL_DATE.format(date);
            case "YYYYMMMM", "YYYYMM", "MMMMYYYY", "MMYYYY" -> MONTH_AND_YEAR.format(date);
            case "TBD" -> null;
            default -> YEAR.format(date);
        };
    }

    private static Properties loadCountryNames() {
        var names = new Properties();
        var resource = new ClassPathResource("company/country-names.properties");
        try (var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            names.load(reader);
            return names;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load country names", e);
        }
    }

}

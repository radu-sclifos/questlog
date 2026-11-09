package com.radusclifos.questlog.company.service;

import com.radusclifos.questlog.company.dto.CompanyDetailDto;
import com.radusclifos.questlog.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
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
import java.util.Locale;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    @Autowired
    private final CompanyRepository companies;

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
                company.getLogoImageId());
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

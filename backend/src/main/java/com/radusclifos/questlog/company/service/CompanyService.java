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
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private static final Properties COUNTRY_NAMES = loadCountryNames();

    @Autowired
    private final CompanyRepository companies;

    public CompanyDetailDto findById(Long id) {
        var company = companies.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
        var countryCode = company.getCountryCode();
        var country = countryCode == null ? null : COUNTRY_NAMES.getProperty(countryCode.toString());
        return new CompanyDetailDto(company.getId(), company.getIgdbId(), company.getName(),
                country, company.getFoundedDate(), company.getFoundedDateFormat(), company.getLogoImageId());
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

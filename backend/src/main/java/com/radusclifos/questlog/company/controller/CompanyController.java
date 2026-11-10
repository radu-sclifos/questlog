package com.radusclifos.questlog.company.controller;

import com.radusclifos.questlog.company.dto.CompanyDetailDto;
import com.radusclifos.questlog.company.dto.CompanyGamePage;
import com.radusclifos.questlog.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {

    @Autowired
    private final CompanyService companies;

    @GetMapping("/{id}")
    public CompanyDetailDto findById(@PathVariable Long id) {
        return companies.findById(id);
    }

    @GetMapping("/{id}/games")
    public CompanyGamePage findGames(@PathVariable Long id, @RequestParam String role,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "24") int size) {
        return companies.findGames(id, role, page, size);
    }

}

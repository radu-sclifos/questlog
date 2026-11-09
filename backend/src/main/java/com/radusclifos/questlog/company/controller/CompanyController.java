package com.radusclifos.questlog.company.controller;

import com.radusclifos.questlog.company.dto.CompanyDetailDto;
import com.radusclifos.questlog.company.service.CompanyService;
import com.radusclifos.questlog.game.dto.GameSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public List<GameSummaryDto> findGames(@PathVariable Long id, @RequestParam String role) {
        return companies.findGames(id, role);
    }

}

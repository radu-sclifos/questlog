package com.radusclifos.questlog.company.controller;

import com.radusclifos.questlog.company.dto.CompanyDetailDto;
import com.radusclifos.questlog.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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

}

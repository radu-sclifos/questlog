package com.radusclifos.questlog.company.dto;

import com.radusclifos.questlog.company.CompanyRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDetailDto {

    private Long id;
    private Long igdbId;
    private String name;
    private String country;
    private String foundedDate;
    private String logoImageId;
    private List<CompanyRole> roles;

}

package com.radusclifos.questlog.company.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

}

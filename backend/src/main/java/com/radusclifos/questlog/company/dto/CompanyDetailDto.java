package com.radusclifos.questlog.company.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDetailDto {

    private Long id;
    private Long igdbId;
    private String name;
    private String country;
    private LocalDate foundedDate;
    private Long foundedDateFormat;
    private String logoImageId;

}

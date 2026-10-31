package com.radusclifos.questlog.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameDto {

    private Long id;
    private Long igdbId;
    private String name;
    private LocalDate firstReleaseDate;
    private String summary;
    private String storyline;
    private String coverImageId;
    private GameTypeDto gameType;
    private List<PlatformDto> platforms;
    private List<GenreDto> genres;
    private List<EngineDto> engines;
    private List<FranchiseDto> franchises;
    private List<SeriesDto> series;
    private List<GameCompanyDto> involvedCompanies;
    private List<GameRelationDto> relations;

}

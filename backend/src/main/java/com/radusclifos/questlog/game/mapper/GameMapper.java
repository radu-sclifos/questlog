package com.radusclifos.questlog.game.mapper;

import com.radusclifos.questlog.game.*;
import com.radusclifos.questlog.game.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GameMapper {

    GameDto toDto(Game game);

    PlatformDto toDto(Platform platform);

    GenreDto toDto(Genre genre);

    EngineDto toDto(Engine engine);

    FranchiseDto toDto(Franchise franchise);

    SeriesDto toDto(Series series);

    GameTypeDto toDto(GameType gameType);

    CompanyDto toDto(Company company);

    GameCompanyDto toDto(GameCompany involvement);

    // Related games stay shallow to avoid cycles in the response
    GameSummaryDto toSummary(Game game);

    @Mapping(target = "game", source = "targetGame")
    GameRelationDto toDto(GameRelation relation);

}

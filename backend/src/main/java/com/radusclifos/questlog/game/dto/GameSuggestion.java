package com.radusclifos.questlog.game.dto;

import java.time.LocalDate;

public record GameSuggestion(
        Long id,
        String name,
        String coverImageId,
        LocalDate firstReleaseDate
) {

}

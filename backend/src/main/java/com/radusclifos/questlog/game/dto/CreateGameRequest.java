package com.radusclifos.questlog.game.dto;

import jakarta.validation.constraints.NotBlank;

//Using record because there's no need to do any changes, we're using this DTO just to transfer data
public record CreateGameRequest(
        @NotBlank String title,
        String coverImageId
) {
}

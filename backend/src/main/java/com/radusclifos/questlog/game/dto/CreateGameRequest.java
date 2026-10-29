package com.radusclifos.questlog.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

//Using record because there's no need to do any changes, we're using this DTO just to transfer data
public record CreateGameRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 255) String coverImageId
) {
}

package com.radusclifos.questlog.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaveFavoritesRequest(
        @NotNull
        @Size(max = 5)
        List<@NotNull @Positive Long> gameIds
) {

}

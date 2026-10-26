package com.radusclifos.questlog.entry.dto;

import com.radusclifos.questlog.entry.EntryOwnership;
import com.radusclifos.questlog.entry.EntryStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveEntryRequest(

        @NotBlank @Size(max = 200) String title,

        @DecimalMin("0.0") @DecimalMax("10.0") Double score,

        @Size(max = 2000) String notes,

        EntryStatus status,

        EntryOwnership ownership,

        Integer playtime

) {
}

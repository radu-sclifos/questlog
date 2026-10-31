package com.radusclifos.questlog.entry.dto;

import com.radusclifos.questlog.entry.EntryOwnership;
import com.radusclifos.questlog.entry.EntryStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaveEntryRequest {

    @NotNull
    @Positive
    private Long gameId;

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private Double score;

    @Size(max = 2000)
    private String notes;

    private EntryStatus status;

    private EntryOwnership ownership;

    private Integer playtime;

}

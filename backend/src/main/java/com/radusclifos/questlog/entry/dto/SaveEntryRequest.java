package com.radusclifos.questlog.entry.dto;

import com.radusclifos.questlog.entry.EntryOwnership;
import com.radusclifos.questlog.entry.EntryStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaveEntryRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private Double score;

    @Size(max = 2000)
    private String notes;

    private EntryStatus status;

    private EntryOwnership ownership;

    private Integer playtime;

}

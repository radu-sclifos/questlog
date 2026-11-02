package com.radusclifos.questlog.entry.dto;

import com.radusclifos.questlog.entry.EntryOwnership;
import com.radusclifos.questlog.entry.EntryStatus;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
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

    @PositiveOrZero
    private Integer achievementsUnlocked;

    @PositiveOrZero
    private Integer achievementsTotal;

    @Positive
    private Long platformId;

    public SaveEntryRequest(Long gameId, Double score, String notes, EntryStatus status,
                            EntryOwnership ownership, Integer playtime) {
        this.gameId = gameId;
        this.score = score;
        this.notes = notes;
        this.status = status;
        this.ownership = ownership;
        this.playtime = playtime;
    }

}

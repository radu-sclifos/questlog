package com.radusclifos.questlog.entry.dto;

import com.radusclifos.questlog.entry.EntryOwnership;
import com.radusclifos.questlog.entry.EntryStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntryDto {

    private Long id;

    private EntryGameDto game;

    private Double score;

    private String notes;

    private EntryStatus status;

    private EntryOwnership ownership;

    private Integer playtime;

}

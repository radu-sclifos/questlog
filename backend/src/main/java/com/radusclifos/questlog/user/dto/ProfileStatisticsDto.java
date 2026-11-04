package com.radusclifos.questlog.user.dto;

import com.radusclifos.questlog.entry.EntryStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileStatisticsDto {

    private long totalGames;
    private long ratedGames;
    private Double averageRating;
    private long finishedGames;
    private Long loggedPlaytime;
    private Map<EntryStatus, Long> statuses;

}

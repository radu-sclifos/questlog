package com.radusclifos.questlog.user.dto;

import com.radusclifos.questlog.entry.dto.EntryGameDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicProfileDto {

    private String username;
    private String displayName;
    private String bio;
    private Instant createdAt;
    private String avatarRevision;
    private ProfileStatisticsDto statistics;
    private List<PlayingGameDto> currentlyPlaying;
    private List<EntryGameDto> favorites;

}

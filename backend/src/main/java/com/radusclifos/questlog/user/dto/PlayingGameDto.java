package com.radusclifos.questlog.user.dto;

import com.radusclifos.questlog.entry.dto.EntryGameDto;
import com.radusclifos.questlog.game.dto.PlatformDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayingGameDto {

    private EntryGameDto game;
    private PlatformDto platform;
    private Integer playtime;

}

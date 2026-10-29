package com.radusclifos.questlog.game.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameResponse {

    private Long id;
    private String title;
    private String coverImageId;

}

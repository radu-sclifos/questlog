package com.radusclifos.questlog.game.dto;

import com.radusclifos.questlog.game.GameRelationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRelationDto {

    private GameRelationType relationType;
    private GameSummaryDto game;

}

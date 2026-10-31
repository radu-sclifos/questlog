package com.radusclifos.questlog.game.dto;

import com.radusclifos.questlog.shared.dto.PageDto;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@NoArgsConstructor
public class GamePage extends PageDto<GameDto> {

    private GamePage(Page<GameDto> page) {
        super(page);
    }

    public static GamePage from(Page<GameDto> page) {
        return new GamePage(page);
    }

}

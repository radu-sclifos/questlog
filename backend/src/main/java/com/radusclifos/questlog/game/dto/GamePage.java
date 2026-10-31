package com.radusclifos.questlog.game.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GamePage {

    private List<GameDto> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static GamePage from(Page<GameDto> page) {
        return new GamePage(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

}

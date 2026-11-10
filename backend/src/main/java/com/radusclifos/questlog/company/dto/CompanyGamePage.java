package com.radusclifos.questlog.company.dto;

import com.radusclifos.questlog.game.dto.GameSummaryDto;
import com.radusclifos.questlog.shared.dto.PageDto;
import org.springframework.data.domain.Page;

public class CompanyGamePage extends PageDto<GameSummaryDto> {

    public CompanyGamePage(Page<GameSummaryDto> page) {
        super(page);
    }
}

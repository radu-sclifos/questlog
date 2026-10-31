package com.radusclifos.questlog.entry.dto;

import com.radusclifos.questlog.shared.dto.PageDto;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@NoArgsConstructor
public class EntryPage extends PageDto<EntryDto> {

    private EntryPage(Page<EntryDto> page) {
        super(page);
    }

    public static EntryPage from(Page<EntryDto> page) {
        return new EntryPage(page);
    }

}

package com.radusclifos.questlog.entry.dto;

import org.springframework.data.domain.Page;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntryPage {

    private List<EntryDto> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static EntryPage from(Page<EntryDto> page) {
        return new EntryPage(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

}

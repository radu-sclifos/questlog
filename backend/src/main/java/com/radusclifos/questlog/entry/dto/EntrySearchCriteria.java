package com.radusclifos.questlog.entry.dto;

import com.radusclifos.questlog.shared.search.SearchCriteria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class EntrySearchCriteria extends SearchCriteria {

    @NotBlank
    @Size(max = 40)
    private String username;

    public EntrySearchCriteria(String username) {
        this.username = username;
    }

    public EntrySearchCriteria(String username, SearchCriteria criteria) {
        super(criteria.getFilters(), criteria.getSorts(), criteria.getPageNumber(), criteria.getPageSize());
        this.username = username;
    }

}

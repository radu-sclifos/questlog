package com.radusclifos.questlog.shared.search;

import org.springframework.data.domain.Sort;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SortCriteria {

    @NotBlank
    private String field;

    @NotNull
    private Sort.Direction direction;

}

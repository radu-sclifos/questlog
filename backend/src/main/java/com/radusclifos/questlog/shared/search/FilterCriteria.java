package com.radusclifos.questlog.shared.search;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FilterCriteria {

    @NotBlank
    private String field;

    @NotNull
    private FilterOperator operator;

    private Object value;

    private List<Object> valueList;

    public FilterCriteria(String field, FilterOperator operator, Object value) {
        this(field, operator, value, null);
    }

    public FilterCriteria(String field, FilterOperator operator, List<Object> valueList) {
        this(field, operator, null, valueList);
    }

}

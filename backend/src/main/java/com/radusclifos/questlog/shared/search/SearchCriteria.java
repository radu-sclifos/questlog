package com.radusclifos.questlog.shared.search;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class SearchCriteria {

    @Min(0)
    private int pageNumber = 0;

    @Min(1)
    private int pageSize = 100;

    @Valid
    @Size(max = 20)
    private List<FilterCriteria> filters = new ArrayList<>();

    @Valid
    @Size(max = 5)
    private List<SortCriteria> sorts = new ArrayList<>();

    public SearchCriteria(List<FilterCriteria> filters, List<SortCriteria> sorts, int pageNumber, int pageSize) {
        setFilters(filters);
        setSorts(sorts);
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public SearchCriteria(List<FilterCriteria> filters, List<SortCriteria> sorts) {
        this(filters, sorts, 0, 100);
    }

    public void setFilters(List<FilterCriteria> filters) {
        this.filters = filters == null ? new ArrayList<>() : new ArrayList<>(List.copyOf(filters));
    }

    public void addFilter(FilterCriteria filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        if (filters == null) {
            filters = new ArrayList<>();
        }
        filters.add(filter);
    }

    public void addSort(SortCriteria sort) {
        Objects.requireNonNull(sort, "sort must not be null");
        if (sorts == null) {
            sorts = new ArrayList<>();
        }
        sorts.add(sort);
    }

    public void setSorts(List<SortCriteria> sorts) {
        this.sorts = sorts == null ? new ArrayList<>() : new ArrayList<>(List.copyOf(sorts));
    }

}

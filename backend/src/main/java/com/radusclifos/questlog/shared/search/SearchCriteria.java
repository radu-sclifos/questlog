package com.radusclifos.questlog.shared.search;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    // ##### Filters
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

    public boolean hasFilter(String field) {
        return filters.stream().anyMatch(filter -> Objects.equals(filter.getField(), field));
    }

    /**
     * Multiple filters may target a field.
     * Returns an unmodifiable list, empty if absent.
     */
    public List<FilterCriteria> findFilters(String field) {
        return filters.stream().filter(filter -> Objects.equals(filter.getField(), field)).toList();
    }

    /**
     * Removes all filters for the field from this criteria.
     * Returns whether any were removed.
     */
    public boolean removeFilters(String field) {
        return filters.removeIf(filter -> Objects.equals(filter.getField(), field));
    }


    // ##### Sorts
    public void setSorts(List<SortCriteria> sorts) {
        this.sorts = sorts == null ? new ArrayList<>() : new ArrayList<>(List.copyOf(sorts));
    }

    public void addSort(SortCriteria sort) {
        Objects.requireNonNull(sort, "sort must not be null");
        if (sorts == null) {
            sorts = new ArrayList<>();
        }
        sorts.add(sort);
    }

    public boolean hasSort(String field) {
        return sorts.stream().anyMatch(sort -> Objects.equals(sort.getField(), field));
    }

    /**
     * Valid criteria allow one sort per field.
     * Optional represents an absent sort.
     */
    public Optional<SortCriteria> findSort(String field) {
        return sorts.stream().filter(sort -> Objects.equals(sort.getField(), field)).findFirst();
    }

    /**
     * Removes the field's sort from this criteria.
     * Returns whether any were removed.
     */
    public boolean removeSort(String field) {
        return sorts.removeIf(sort -> Objects.equals(sort.getField(), field));
    }

}

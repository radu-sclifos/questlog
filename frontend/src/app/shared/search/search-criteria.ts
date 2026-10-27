import { FilterCriteria } from './filter-criteria';
import { SortCriteria } from './sort-criteria';

export class SearchCriteria {
  /** Zero-based page index (backend default: 0). */
  pageNumber?: number;

  /** Between 5 and 100 (backend default: 100). */
  pageSize?: number;

  /** At most 20 filters on direct entity fields. */
  filters: FilterCriteria[] = [];

  /** At most 5 sorts on direct entity fields. */
  sorts: SortCriteria[] = [];

  addFilter(filter: FilterCriteria): this {
    this.filters.push(filter);
    return this;
  }

  setFilter(filters: FilterCriteria[]) {
    this.filters = filters;
  }

  cleanFilters(): void {
    this.filters = [];
  }

  addSort(sort: SortCriteria): this {
    this.sorts.push(sort);
    return this;
  }

  setSort(sorts: SortCriteria[]) {
    this.sorts = sorts;
  }

  hasFilters(): boolean {
    return this.filters.length > 0;
  }
}

import { Component, inject, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, ValueChangeEvent } from '@angular/forms';
import { filter, of, switchMap, timer } from 'rxjs';
import { EntryStatus } from '../../models/entry';
import { ENTRY_STATUS_LABELS } from '../../models/entry-labels';
import { SearchCriteria } from '../../../../shared/search/search-criteria';
import { SortDirection } from '../../../../shared/search/sort-criteria';
import { FilterOperator } from '../../../../shared/search/filter-criteria';

@Component({
  selector: 'app-entry-list-header',
  imports: [ReactiveFormsModule],
  templateUrl: './entry-list-header.html',
  host: { class: 'block' },
})
export class EntryListHeader {
  readonly criteriaChange = output<SearchCriteria>();

  private readonly fb = inject(FormBuilder).nonNullable;
  protected readonly form = this.fb.group({
    search: '',
    status: this.fb.control<EntryStatus | ''>(''),
    sortBy: this.fb.control<'status' | 'title' | 'score'>('status'),
    sortDirection: SortDirection.ASC,
  });

  protected readonly statuses = Object.values(EntryStatus);
  protected readonly statusLabels = ENTRY_STATUS_LABELS;
  protected readonly SortDirection = SortDirection;

  constructor() {
    this.form.events
      .pipe(
        filter((event) => event instanceof ValueChangeEvent),
        // Only typing waits
        switchMap((event) => (event.source === this.form.controls.search ? timer(300) : of(0))),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        this.criteriaChange.emit(this.buildCriteria());
      });
  }

  protected hasFilters(): boolean {
    const { search, status } = this.form.getRawValue();
    return !!(search.trim() || status);
  }

  reset(): void {
    this.form.reset();
  }

  protected reverseOrder(): void {
    const control = this.form.controls.sortDirection;
    control.setValue(control.value === SortDirection.ASC ? SortDirection.DESC : SortDirection.ASC);
  }

  protected clearFilters(): void {
    this.form.patchValue({ search: '', status: '' });
  }

  private buildCriteria(): SearchCriteria {
    const { search, status, sortBy, sortDirection } = this.form.getRawValue();
    const criteria = new SearchCriteria();

    if (search.trim()) {
      criteria.addFilter({
        field: 'title',
        operator: FilterOperator.CONTAINS,
        value: search.trim(),
      });
    }

    if (status) {
      criteria.addFilter({
        field: 'status',
        operator: FilterOperator.EQUAL,
        value: status,
      });
    }

    this.buildSort(criteria, sortBy, sortDirection);

    return criteria;
  }

  private buildSort(criteria: SearchCriteria, sortBy: string, sortDirection: SortDirection): void {
    // Sorts are applied in list order: the selected field comes first,
    // then title ASC breaks ties unless title is already the selected field
    criteria.addSort({
      field: sortBy,
      direction: sortDirection,
      nullsLast: sortBy === 'score',
    });

    if (sortBy !== 'title') {
      criteria.addSort({
        field: 'title',
        direction: SortDirection.ASC,
      });
    }
  }
}

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
    sortBy: this.fb.control<'title' | 'score'>('title'),
    sortDirection: SortDirection.ASC,
  });

  protected readonly statuses = Object.values(EntryStatus);
  protected readonly statusLabels = ENTRY_STATUS_LABELS;
  protected readonly SortDirection = SortDirection;

  constructor() {
    this.form.events.pipe(
      filter((event) => event instanceof ValueChangeEvent),
      // Only typing waits
      switchMap((event) => event.source === this.form.controls.search ? timer(300) : of(0)),
      takeUntilDestroyed(),
    ).subscribe(() => {
      this.criteriaChange.emit(this.buildCriteria());
    });
  }

  protected hasFilters(): boolean {
    const { search, status } = this.form.getRawValue();
    return !!(search.trim() || status);
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

    criteria.addSort({
      field: sortBy,
      direction: sortDirection,
    });

    return criteria;
  }
}

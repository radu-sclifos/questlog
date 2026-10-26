import { Component, computed, input, model } from '@angular/core';
import { EntryStatus } from '../../models/entry';
import { ENTRY_STATUS_LABELS } from '../../models/entry-labels';

export interface EntryListQuery {
  search: string;
  status: EntryStatus | '';
  sortBy: 'title' | 'score';
  sortDirection: 'asc' | 'desc';
}

export const INITIAL_ENTRY_LIST_QUERY: EntryListQuery = {
  search: '',
  status: '',
  sortBy: 'title',
  sortDirection: 'asc',
};

@Component({
  selector: 'app-entry-list-header',
  templateUrl: './entry-list-header.html',
  host: { class: 'block' },
})
export class EntryListHeader {
  readonly query = model<EntryListQuery>({ ...INITIAL_ENTRY_LIST_QUERY });
  readonly resultCount = input.required<number>();
  protected readonly statuses = Object.values(EntryStatus);
  protected readonly statusLabels = ENTRY_STATUS_LABELS;
  protected readonly hasFilters = computed(() => !!(this.query().search || this.query().status));

  protected update<K extends keyof EntryListQuery>(key: K, value: EntryListQuery[K]): void {
    this.query.update((query) => ({ ...query, [key]: value }));
  }

  protected updateStatus(value: string): void {
    if (value === '' || this.statuses.some((status) => status === value)) {
      const status = this.statuses.find((status) => status === value) ?? '';
      this.update('status', status);
    }
  }

  protected updateSortBy(value: string): void {
    if (value === 'title' || value === 'score') {
      this.update('sortBy', value);
    }
  }

  protected reverseOrder(): void {
    this.query.update((query) => ({
      ...query,
      sortDirection: query.sortDirection === 'asc' ? 'desc' : 'asc',
    }));
  }

  protected clearFilters(): void {
    this.query.update((query) => ({ ...query, search: '', status: '' }));
  }
}

import { EntryListHeader, INITIAL_ENTRY_LIST_QUERY, EntryListQuery } from '../../components/entry-list-header/entry-list-header';
import { Component, computed, inject, signal } from '@angular/core';
import { EntryCard } from '../../components/entry-card/entry-card';
import { EntryNavbar } from '../../components/entry-navbar/entry-navbar';
import { EntryEditorDialog } from '../../components/entry-editor-dialog/entry-editor-dialog';
import { EntryDeleteDialog } from '../../components/entry-delete-dialog/entry-delete-dialog';
import { EntryService } from '../../services/entry.service';
import { Entry, EntryStatus } from '../../models/entry';
import { ToastService } from '../../../../shared/toast/toast.service';

@Component({
  selector: 'app-entry-list',
  imports: [EntryListHeader, EntryCard, EntryNavbar, EntryEditorDialog, EntryDeleteDialog],
  templateUrl: './entry-list.html',
  styleUrl: './entry-list.scss',
})
export class EntryList {
  private readonly entryService = inject(EntryService);
  private readonly toast = inject(ToastService);

  protected readonly entries = signal<Entry[]>([]);
  protected readonly query = signal<EntryListQuery>({ ...INITIAL_ENTRY_LIST_QUERY });
  protected readonly visibleEntries = computed(() => {
    const query = this.query();
    const search = query.search.trim().toLocaleLowerCase();
    const direction = query.sortDirection === 'asc' ? 1 : -1;
    return this.entries()
      .filter(entry => entry.title.toLocaleLowerCase().includes(search)
        && (!query.status || (entry.status ?? EntryStatus.UNPLAYED) === query.status))
      .sort((a, b) => {
        if (query.sortBy === 'score') {
          if (a.score === null && b.score !== null) return 1;
          if (b.score === null && a.score !== null) return -1;
          const scoreOrder = (a.score ?? 0) - (b.score ?? 0);
          if (scoreOrder) return scoreOrder * direction;
        }
        return a.title.localeCompare(b.title, undefined, { sensitivity: 'base', numeric: true })
          * (query.sortBy === 'title' ? direction : 1) || a.id - b.id;
      });
  });
  protected readonly canEdit = true;
  protected readonly errorMessage = signal<string | null>(null);

  protected onEntrySaved(entry: Entry): void {
    const isUpdate = this.entries().some(existing => existing.id === entry.id);
    this.entries.update((entries) =>
      entries.some(existing => existing.id === entry.id)
        ? entries.map(existing => existing.id === entry.id ? entry : existing)
        : [...entries, entry]
    );
    this.errorMessage.set(null);
    this.toast.success(isUpdate ? 'Game updated.' : 'Game added.');
  }

  protected onEntryDeleted(entry: Entry): void {
    this.entries.update(entries => entries.filter(existing => existing.id !== entry.id));
    this.errorMessage.set(null);
    this.toast.success('Game deleted.');
  }

  ngOnInit(): void {
    this.loadEntries();
  }

  private loadEntries(): void {
    this.entryService.findAll().subscribe({
      next: (entries) => {
        this.entries.set(entries);
      },
      error: (error) => {
        this.errorMessage.set('Error during load entries list');
        console.error('Error during load entries list:', error);
      },
    });
  }

}


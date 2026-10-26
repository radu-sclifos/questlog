import { Component, inject, signal } from '@angular/core';
import { EntryCard } from '../../components/entry-card/entry-card';
import { EntryNavbar } from '../../components/entry-navbar/entry-navbar';
import { EntryEditorDialog } from '../../components/entry-editor-dialog/entry-editor-dialog';
import { EntryDeleteDialog } from '../../components/entry-delete-dialog/entry-delete-dialog';
import { EntryService } from '../../services/entry.service';
import { Entry } from '../../models/entry';
import { ToastService } from '../../../../shared/toast/toast.service';

@Component({
  selector: 'app-entry-list',
  imports: [EntryCard, EntryNavbar, EntryEditorDialog, EntryDeleteDialog],
  templateUrl: './entry-list.html',
  styleUrl: './entry-list.scss',
})
export class EntryList {
  private readonly entryService = inject(EntryService);
  private readonly toast = inject(ToastService);

  protected readonly entries = signal<Entry[]>([]);
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


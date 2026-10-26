import { Component, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Entry, EntryOwnership, EntryStatus, SaveEntryRequest } from '../../models/entry';
import { ENTRY_OWNERSHIP_LABELS, ENTRY_STATUS_LABELS } from '../../models/entry-labels';
import { EntryService } from '../../services/entry.service';

@Component({
  selector: 'app-entry-editor-dialog',
  imports: [ReactiveFormsModule],
  templateUrl: './entry-editor-dialog.html',
})
export class EntryEditorDialog {
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private readonly entryService = inject(EntryService);

  readonly savedEntry = output<Entry>();
  //meant to block the saving button and not allow the user to add multiple times the same Entry
  protected readonly saving = signal(false);
  //if there's an Entry inside editingEntry that means that we're editing, so we apply the corresponding behavior
  protected readonly editingEntry = signal<Entry | null>(null);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly statuses = Object.values(EntryStatus);
  protected readonly ownerships = Object.values(EntryOwnership);
  protected readonly statusLabels = ENTRY_STATUS_LABELS;
  protected readonly ownershipLabels = ENTRY_OWNERSHIP_LABELS;

  protected readonly form = new FormGroup({
    title: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(200), Validators.pattern(/\S/)],
    }),
    score: new FormControl<number | null>(null, [Validators.min(0), Validators.max(10)]),
    status: new FormControl<EntryStatus | null>(null),
    ownership: new FormControl<EntryOwnership | null>(null),
    playtime: new FormControl<number | null>(null, [Validators.min(0), Validators.pattern(/^\d+$/)]),
    notes: new FormControl('', { nonNullable: true, validators: Validators.maxLength(2000) }),
  });

  open(entry: Entry | null = null): void {
    if (this.dialog().nativeElement.open) {
      return;
    }
    this.editingEntry.set(entry);
    this.form.reset(entry ? {
      title: entry.title,
      score: entry.score,
      status: entry.status,
      ownership: entry.ownership,
      playtime: entry.playtime,
      notes: entry.notes ?? '',
    } : {});
    this.errorMessage.set(null);
    this.dialog().nativeElement.showModal();
  }

  protected close(): void {
    if (!this.saving()) {
      this.dialog().nativeElement.close();
    }
  }

  protected onCancel(event: Event): void {
    if (this.saving()) {
      event.preventDefault();
    }
  }

  protected submit(): void {
    if (this.saving()) {
      return;
    }
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }

    const value = this.form.getRawValue();
    this.saving.set(true);
    this.errorMessage.set(null);

    const request: SaveEntryRequest = {
      ...value,
      title: value.title.trim(),
      notes: value.notes.trim() || null,
    };

    const entry = this.editingEntry();
    const saveRequest = entry
      ? this.entryService.update(entry.id, request)
      : this.entryService.create(request);

    saveRequest
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (entry) => {
          this.dialog().nativeElement.close();
          this.savedEntry.emit(entry);
        },
        error: () => this.errorMessage.set(
          entry ? 'Unable to update the game. Please try again.' : 'Unable to add the game. Please try again.'
        ),
      });
  }
}

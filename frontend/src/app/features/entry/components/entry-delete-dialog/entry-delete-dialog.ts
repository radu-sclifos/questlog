import { Component, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { finalize } from 'rxjs';
import { Entry } from '../../models/entry';
import { EntryService } from '../../services/entry.service';

@Component({
  selector: 'app-entry-delete-dialog',
  templateUrl: './entry-delete-dialog.html',
})
export class EntryDeleteDialog {
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private readonly entryService = inject(EntryService);

  readonly deletedEntry = output<Entry>();
  protected readonly entry = signal<Entry | null>(null);
  protected readonly deleting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  open(entry: Entry): void {
    if (this.dialog().nativeElement.open || this.deleting()) {
      return;
    }
    this.entry.set(entry);
    this.errorMessage.set(null);
    this.dialog().nativeElement.showModal();
  }

  protected close(): void {
    if (!this.deleting()) {
      this.dialog().nativeElement.close();
    }
  }

  protected onCancel(event: Event): void {
    if (this.deleting()) {
      event.preventDefault();
    }
  }

  protected confirm(): void {
    const entry = this.entry();
    if (!entry || this.deleting()) {
      return;
    }

    this.deleting.set(true);
    this.errorMessage.set(null);
    this.entryService.delete(entry.id)
      .pipe(finalize(() => this.deleting.set(false)))
      .subscribe({
        next: () => {
          this.dialog().nativeElement.close();
          this.deletedEntry.emit(entry);
        },
        error: () => this.errorMessage.set('Unable to delete the game. Please try again.'),
      });
  }
}

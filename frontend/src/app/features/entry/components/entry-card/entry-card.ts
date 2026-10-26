import { Component, input, output, signal } from '@angular/core';
import { Entry, EntryStatus } from '../../models/entry';
import { ENTRY_OWNERSHIP_LABELS, ENTRY_STATUS_LABELS } from '../../models/entry-labels';

@Component({
  selector: 'app-entry-card',
  templateUrl: './entry-card.html',
  styleUrl: './entry-card.scss',
  host: { class: 'block' }
})
export class EntryCard {
  readonly entry = input.required<Entry>();
  readonly canEdit = input(false);
  readonly editRequested = output<Entry>();
  readonly deleteRequested = output<Entry>();

  protected readonly expanded = signal(false);
  protected readonly entryStatus = EntryStatus;
  protected readonly statusLabels = ENTRY_STATUS_LABELS;
  protected readonly ownershipLabels = ENTRY_OWNERSHIP_LABELS;

  protected readonly statusClasses: Record<EntryStatus, string> = {
    [EntryStatus.PLAYING]: 'badge-success',
    [EntryStatus.ON_HOLD]: 'badge-warning',
    [EntryStatus.DROPPED]: 'badge-error',
    [EntryStatus.COMPLETED]: 'badge-info',
    [EntryStatus.BEATEN]: 'badge-info',
    [EntryStatus.ENDLESS]: 'badge-success',
    [EntryStatus.UNPLAYED]: 'badge-neutral',
    [EntryStatus.PLANNED]: 'badge-neutral'
  };

  protected toggleExpanded(): void {
    this.expanded.update(value => !value);
  }
}

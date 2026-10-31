import { Component, computed, input, output, signal } from '@angular/core';
import { Entry } from '../../models/entry';
import { ENTRY_OWNERSHIP_LABELS } from '../../models/entry-labels';

import { EntryStatusBadge } from '../entry-status-badge/entry-status-badge';

@Component({
  imports: [EntryStatusBadge],
  selector: 'app-entry-card',
  templateUrl: './entry-card.html',
  styleUrl: './entry-card.scss',
  host: { class: 'block' },
})
export class EntryCard {
  readonly entry = input.required<Entry>();
  readonly canEdit = input(false);
  readonly editRequested = output<Entry>();
  readonly deleteRequested = output<Entry>();

  protected readonly expanded = signal(false);
  protected readonly ownershipLabels = ENTRY_OWNERSHIP_LABELS;

  private readonly failedCoverId = signal<string | null>(null);

  protected readonly coverUrl = computed(() => {
    const imageId = this.entry().game.coverImageId;
    return imageId && imageId !== this.failedCoverId()
      ? `https://images.igdb.com/igdb/image/upload/t_cover_small/${encodeURIComponent(imageId)}.jpg`
      : null;
  });

  protected onCoverError(): void {
    this.failedCoverId.set(this.entry().game.coverImageId);
  }

  protected toggleExpanded(): void {
    this.expanded.update((value) => !value);
  }
}

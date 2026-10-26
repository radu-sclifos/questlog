import { Component, computed, input } from '@angular/core';
import { Badge, BadgeSize, BadgeTone } from '../../../../shared/badge/badge';
import { EntryStatus } from '../../models/entry';
import { ENTRY_STATUS_LABELS } from '../../models/entry-labels';

const STATUS_TONES = {
  [EntryStatus.PLAYING]: 'sky',
  [EntryStatus.BEATEN]: 'green',
  [EntryStatus.COMPLETED]: 'violet',
  [EntryStatus.UNPLAYED]: 'slate',
  [EntryStatus.ON_HOLD]: 'amber',
  [EntryStatus.ENDLESS]: 'beige',
  [EntryStatus.DROPPED]: 'rose',
} as const satisfies Record<EntryStatus, BadgeTone>;

@Component({
  selector: 'app-entry-status-badge',
  imports: [Badge],
  template: `<app-badge [tone]="tone()" [size]="size()">{{ label() }}</app-badge>`,
  host: { class: 'align-middle' },
})
export class EntryStatusBadge {
  readonly status = input.required<EntryStatus | null>();
  readonly size = input<BadgeSize>('md');

  private readonly effectiveStatus = computed(() => this.status() ?? EntryStatus.UNPLAYED);

  protected readonly tone = computed(() => STATUS_TONES[this.effectiveStatus()]);
  protected readonly label = computed(() => ENTRY_STATUS_LABELS[this.effectiveStatus()]);
}

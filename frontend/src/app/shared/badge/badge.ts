import { Component, input } from '@angular/core';

export type BadgeTone = 'green' | 'sky' | 'violet' | 'slate' | 'blue' | 'amber' | 'teal' | 'rose' | 'beige';
export type BadgeSize = 'sm' | 'md';

@Component({
  selector: 'app-badge',
  template: `<span
    class="badge questlog-badge"
    [class.badge-sm]="size() === 'sm'"
    [attr.data-tone]="tone()"
    ><ng-content
  /></span>`,
  styleUrl: './badge.scss',
  host: { class: 'inline-flex align-middle' },
})
export class Badge {
  readonly tone = input<BadgeTone>('slate');
  readonly size = input<BadgeSize>('md');
}

import { Component, effect, inject, input, signal, viewChild } from '@angular/core';
import { AuthService } from '../../../auth/services/auth.service';
import { EntryService } from '../../../entry/services/entry.service';
import { Entry } from '../../../entry/models/entry';
import { EntrySearchCriteria } from '../../../entry/models/entry-search-criteria';
import { FilterOperator } from '../../../../shared/search/filter-criteria';
import { EntryStatusBadge } from '../../../entry/components/entry-status-badge/entry-status-badge';
import { EntryEditorDialog } from '../../../entry/components/entry-editor-dialog/entry-editor-dialog';
import { ToastService } from '../../../../shared/toast/toast.service';
import { Game } from '../../models/game';

@Component({
  selector: 'app-game-journey',
  imports: [EntryStatusBadge, EntryEditorDialog],
  templateUrl: './game-journey.html',
  styleUrl: './game-journey.scss',
})
export class GameJourney {
  readonly game = input.required<Game>();
  protected readonly auth = inject(AuthService);
  private readonly entries = inject(EntryService);
  private readonly toast = inject(ToastService);
  private readonly editor = viewChild(EntryEditorDialog);
  protected readonly entry = signal<Entry | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal(false);

  constructor() {
    effect((onCleanup) => {
      const user = this.auth.user();
      if (!user) {
        return;
      }

      const game = this.game();
      this.entry.set(null);
      this.error.set(false);
      this.loading.set(!!user);

      // Always use the current session regardless of the library that linked here
      const criteria = new EntrySearchCriteria(user.username);
      criteria.pageSize = 1;
      criteria.addFilter({ field: 'game.id', operator: FilterOperator.EQUAL, value: game.id });

      const subscription = this.entries.search(criteria).subscribe({
        next: (page) => {
          this.entry.set(page.items[0] ?? null);
          this.loading.set(false);
        },
        error: () => {
          this.error.set(true);
          this.loading.set(false);
        },
      });

      onCleanup(() => subscription.unsubscribe());
    });
  }

  protected edit(): void {
    const entry = this.entry();
    if (entry) this.editor()?.open(entry);
    else this.editor()?.openForGame(this.game());
  }

  protected onSaved(entry: Entry): void {
    this.toast.success(this.entry() ? 'Game updated.' : 'Game added.');
    this.entry.set(entry);
  }
}

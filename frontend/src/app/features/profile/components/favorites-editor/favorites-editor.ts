import {
  Component,
  DestroyRef,
  ElementRef,
  inject,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, Subject, switchMap, timer } from 'rxjs';
import { GameService } from '../../../games/services/game.service';
import { GameSuggestion } from '../../../games/models/game-suggestion';
import { ToastService } from '../../../../shared/toast/toast.service';
import { ProfileService } from '../../services/profile.service';

@Component({ selector: 'app-favorites-editor', templateUrl: './favorites-editor.html' })
export class FavoritesEditor {
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  private readonly profiles = inject(ProfileService);
  private readonly games = inject(GameService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly queries = new Subject<string>();
  readonly saved = output<void>();

  protected readonly selected = signal<GameSuggestion[]>([]);
  protected readonly suggestions = signal<GameSuggestion[]>([]);
  protected readonly query = signal('');
  protected readonly saving = signal(false);
  protected readonly searching = signal(false);
  protected readonly searchError = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.queries
      .pipe(
        switchMap((query) => {
          this.suggestions.set([]);
          this.searchError.set(false);
          this.searching.set(false);

          if (query.length < 3 || this.selected().length >= 5) {
            return of([]);
          }

          this.searching.set(true);
          return timer(300).pipe(
            switchMap(() => this.games.suggestions(query)),
            catchError(() => {
              this.searchError.set(true);
              return of([]);
            }),
            finalize(() => this.searching.set(false)),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((games) => this.suggestions.set(games));
  }

  open(favorites: GameSuggestion[]): void {
    this.selected.set([...favorites]);
    this.setQuery('');
    this.error.set(null);
    this.dialog().nativeElement.showModal();
  }

  protected close(): void {
    if (this.saving()) {
      return;
    }
    this.setQuery('');
    this.dialog().nativeElement.close();
  }

  protected setQuery(value: string): void {
    this.query.set(value);
    this.queries.next(value.trim());
  }

  protected isSelected(id: number): boolean {
    return this.selected().some((game) => game.id === id);
  }

  protected add(game: GameSuggestion): void {
    if (this.saving() || this.selected().length >= 5 || this.isSelected(game.id)) {
      return;
    }
    this.selected.update((games) => [...games, game]);
    this.setQuery('');
  }

  protected remove(id: number): void {
    if (!this.saving()) {
      this.selected.update((games) => games.filter((game) => game.id !== id));
    }
  }

  protected move(index: number, offset: number): void {
    const games = [...this.selected()];
    const destination = index + offset;
    if (this.saving() || destination < 0 || destination >= games.length) {
      return;
    }

    [games[index], games[destination]] = [games[destination], games[index]];
    this.selected.set(games);
  }

  protected save(): void {
    if (this.saving()) {
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.setQuery('');

    this.profiles
      .saveFavorites(this.selected().map((game) => game.id))
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: () => {
          this.dialog().nativeElement.close();
          this.saved.emit();
          this.toast.success('Favorite games updated.');
        },
        error: () => this.error.set('Unable to save your favorites. Please try again.'),
      });
  }
}

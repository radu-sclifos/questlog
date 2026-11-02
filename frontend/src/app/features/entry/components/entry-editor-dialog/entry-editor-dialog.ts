import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, finalize, of, Subject, switchMap, timer } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GameService } from '../../../games/services/game.service';
import { Platform } from '../../../games/models/platform';
import { sortPlatforms } from '../../../games/models/platform-order';
import { GameSuggestion } from '../../../games/models/game-suggestion';
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
  private readonly gameService = inject(GameService);
  private readonly suggestionQueries = new Subject<string | null>();

  private readonly platformQueries = new Subject<number | null>();
  protected readonly platforms = signal<Platform[]>([]);
  protected readonly platformsLoading = signal(false);
  protected readonly platformsError = signal(false);

  protected readonly suggestions = signal<GameSuggestion[]>([]);
  protected readonly suggestionsLoading = signal(false);
  protected readonly suggestionsError = signal(false);
  protected readonly titleFocused = signal(false);
  protected readonly highlightedIndex = signal(-1);

  readonly savedEntry = output<Entry>();
  //meant to block the saving button and not allow the user to add multiple times the same Entry
  protected readonly saving = signal(false);
  protected readonly showMoreInfo = signal(false);
  //if there's an Entry inside editingEntry that means that we're editing, so we apply the corresponding behavior
  protected readonly editingEntry = signal<Entry | null>(null);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly statuses = Object.values(EntryStatus);
  protected readonly ownerships = Object.values(EntryOwnership);
  protected readonly statusLabels = ENTRY_STATUS_LABELS;
  protected readonly ownershipLabels = ENTRY_OWNERSHIP_LABELS;

  protected readonly form = new FormGroup({
    platformId: new FormControl<number | null>(null),
    gameId: new FormControl<number | null>(null, [Validators.required, Validators.min(1)]),
    title: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/)],
    }),
    score: new FormControl<number | null>(null, [
      Validators.min(0),
      Validators.max(10),
      Validators.pattern(/^\d+(?:\.5)?$/),
    ]),
    status: new FormControl<EntryStatus | null>(null),
    ownership: new FormControl<EntryOwnership | null>(null),
    playtime: new FormControl<number | null>(null, [
      Validators.min(0),
      Validators.pattern(/^\d+$/),
    ]),
    achievementsUnlocked: new FormControl<number | null>(null, [
      Validators.min(0), Validators.max(2147483647), Validators.pattern(/^\d+$/),
    ]),
    achievementsTotal: new FormControl<number | null>(null, [
      Validators.min(0), Validators.max(2147483647), Validators.pattern(/^\d+$/),
    ]),
    notes: new FormControl('', { nonNullable: true, validators: Validators.maxLength(2000) }),
  }, { validators: (control) => {
    const { achievementsUnlocked, achievementsTotal } = control.value;
    return achievementsUnlocked != null && achievementsTotal != null && achievementsUnlocked > achievementsTotal
      ? { achievementsOrder: true } : null;
  } });

  constructor() {
    this.suggestionQueries
      .pipe(
        switchMap((name) => {
          this.suggestionsLoading.set(false);
          this.suggestions.set([]);
          this.suggestionsError.set(false);
          this.highlightedIndex.set(-1);
          if (!name || name.length < 3) {
            return of([] as GameSuggestion[]);
          }

          // Cancel the previous request immediately, then wait for typing to pause
          this.suggestionsLoading.set(true);

          return timer(300).pipe(
            switchMap(() => {
              return this.gameService.suggestions(name).pipe(
                catchError(() => {
                  this.suggestionsError.set(true);
                  return of([] as GameSuggestion[]);
                }),
              );
            }),
            finalize(() => this.suggestionsLoading.set(false)),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((games) => this.suggestions.set(games));

    // Changing or closing the game cancels any pending platform request.
    this.platformQueries
      .pipe(
        switchMap((id) => {
          this.platforms.set([]);
          this.platformsError.set(false);
          this.platformsLoading.set(id !== null);
          if (id === null) {
            return of(null);
          }
          return this.gameService.platforms(id).pipe(
            catchError(() => {
              this.platformsError.set(true);
              return of(null);
            }),
            finalize(() => this.platformsLoading.set(false)),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((platforms) =>
        this.platforms.set(sortPlatforms(platforms ?? [])),
      );

    this.form.controls.title.valueChanges.pipe(takeUntilDestroyed()).subscribe((name) => {
      if (this.editingEntry()) { // Can't search for games if editing
        return;
      }

      // Typed text is not a selection, even when it matches the old name
      this.form.controls.gameId.setValue(null);
      this.form.controls.platformId.setValue(null);
      this.platformQueries.next(null);
      this.suggestionQueries.next(name.trim());
    });
  }

  protected selectGame(game: GameSuggestion): void {
    if (this.editingEntry()) { // Can't select game if editing
      return;
    }

    this.suggestionQueries.next(null);
    this.form.patchValue({ title: game.name, gameId: game.id }, { emitEvent: false });
    this.form.controls.platformId.setValue(null);
    this.platformQueries.next(game.id);
    this.titleFocused.set(false);
  }

  protected onTitleFocusOut(event: FocusEvent): void {
    const container = event.currentTarget as HTMLElement;
    if (!container.contains(event.relatedTarget as Node | null)) this.titleFocused.set(false);
  }

  protected onTitleKeydown(event: KeyboardEvent): void {
    if (this.editingEntry()) { // Can't change title if editing
      return;
    }

    const suggestedGames = this.suggestions();

    if (!this.titleFocused()) {
      return;
    }

    if (event.key === 'Escape') {
      event.preventDefault();
      event.stopPropagation();
      this.titleFocused.set(false);
    } else if (suggestedGames.length && (event.key === 'ArrowDown' || event.key === 'ArrowUp')) {
      event.preventDefault();
      const step = event.key === 'ArrowDown' ? 1 : -1;
      this.highlightedIndex.update((index) => (index + step + suggestedGames.length) % suggestedGames.length);
    } else if (event.key === 'Enter' && suggestedGames.length) {
      event.preventDefault();
      const game = suggestedGames[Math.max(0, this.highlightedIndex())];
      if (game) {
        this.selectGame(game);
      }
    }
  }

  open(entry: Entry | null = null): void {
    if (this.dialog().nativeElement.open) {
      return;
    }
    this.suggestionQueries.next(null);
    this.titleFocused.set(false);
    this.editingEntry.set(entry);
    this.showMoreInfo.set(false);
    this.form.reset(
      entry
        ? {
            title: entry.game.name,
            gameId: entry.game.id,
            platformId: entry.platform?.id ?? null,
            score: entry.score,
            status: entry.status,
            ownership: entry.ownership,
            playtime: entry.playtime,
            achievementsUnlocked: entry.achievementsUnlocked,
            achievementsTotal: entry.achievementsTotal,
            notes: entry.notes ?? '',
          }
        : {},
      { emitEvent: false },
    );
    this.platformQueries.next(entry?.game.id ?? null);
    this.errorMessage.set(null);
    this.dialog().nativeElement.showModal();
  }

  protected close(): void {
    if (!this.saving()) {
      this.platformQueries.next(null);
      this.suggestionQueries.next(null);
      this.dialog().nativeElement.close();
    }
  }

  protected onCancel(event: Event): void {
    if (this.saving()) {
      event.preventDefault();
    } else {
      this.platformQueries.next(null);
      this.suggestionQueries.next(null);
    }
  }

  protected retryPlatforms(): void {
    this.platformQueries.next(this.form.controls.gameId.value);
  }

  protected normalizeScore(): void {
    const control = this.form.controls.score;
    if (control.value !== null) {
      control.setValue(Math.max(0, Math.min(10, Math.floor(control.value * 2) / 2)));
    }
  }

  protected submit(): void {
    if (this.saving()) {
      return;
    }
    this.form.markAllAsTouched();
    if (this.form.hasError('achievementsOrder') || this.form.controls.achievementsUnlocked.invalid
        || this.form.controls.achievementsTotal.invalid) {
      this.showMoreInfo.set(true);
    }
    if (this.form.invalid || this.form.controls.gameId.value === null) {
      return;
    }

    const value = this.form.getRawValue();
    this.saving.set(true);
    this.errorMessage.set(null);

    const request: SaveEntryRequest = {
      gameId: this.form.controls.gameId.value,
      platformId: value.platformId,
      score: value.score,
      status: value.status,
      ownership: value.ownership,
      playtime: value.playtime,
      achievementsUnlocked: value.achievementsUnlocked,
      achievementsTotal: value.achievementsTotal,
      notes: value.notes.trim() || null,
    };

    const entry = this.editingEntry();
    const saveRequest = entry
      ? this.entryService.update(entry.id, request)
      : this.entryService.create(request);

    saveRequest.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (entry) => {
        this.dialog().nativeElement.close();
        this.savedEntry.emit(entry);
      },
      error: (error: unknown) =>
        this.errorMessage.set(
          error instanceof HttpErrorResponse && error.status === 409
            ? 'This game is already in your library.'
            : entry
              ? 'Unable to update the game. Please try again.'
              : 'Unable to add the game. Please try again.',
        ),
    });
  }
}

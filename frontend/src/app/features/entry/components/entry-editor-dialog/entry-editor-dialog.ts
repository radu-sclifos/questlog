import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, inject, output, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, finalize, of, Subject, switchMap, timer } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GameService } from '../../../games/services/game.service';
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

  protected readonly suggestions = signal<GameSuggestion[]>([]);
  protected readonly suggestionsLoading = signal(false);
  protected readonly suggestionsError = signal(false);
  protected readonly titleFocused = signal(false);
  protected readonly highlightedIndex = signal(-1);

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
    gameId: new FormControl<number | null>(null, [Validators.required, Validators.min(1)]),
    title: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/)],
    }),
    score: new FormControl<number | null>(null, [Validators.min(0), Validators.max(10)]),
    status: new FormControl<EntryStatus | null>(null),
    ownership: new FormControl<EntryOwnership | null>(null),
    playtime: new FormControl<number | null>(null, [
      Validators.min(0),
      Validators.pattern(/^\d+$/),
    ]),
    notes: new FormControl('', { nonNullable: true, validators: Validators.maxLength(2000) }),
  });

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

    this.form.controls.title.valueChanges.pipe(takeUntilDestroyed()).subscribe((name) => {
      // Typed text is not a selection, even when it matches the old name
      this.form.controls.gameId.setValue(null);
      this.suggestionQueries.next(name.trim());
    });
  }

  protected selectGame(game: GameSuggestion): void {
    this.suggestionQueries.next(null);
    this.form.patchValue({ title: game.name, gameId: game.id }, { emitEvent: false });
    this.titleFocused.set(false);
  }

  protected onTitleFocusOut(event: FocusEvent): void {
    const container = event.currentTarget as HTMLElement;
    if (!container.contains(event.relatedTarget as Node | null)) this.titleFocused.set(false);
  }

  protected onTitleKeydown(event: KeyboardEvent): void {
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
    this.form.reset(
      entry
        ? {
            title: entry.game.name,
            gameId: entry.game.id,
            score: entry.score,
            status: entry.status,
            ownership: entry.ownership,
            playtime: entry.playtime,
            notes: entry.notes ?? '',
          }
        : {},
      { emitEvent: false },
    );
    this.errorMessage.set(null);
    this.dialog().nativeElement.showModal();
  }

  protected close(): void {
    if (!this.saving()) {
      this.suggestionQueries.next(null);
      this.dialog().nativeElement.close();
    }
  }

  protected onCancel(event: Event): void {
    if (this.saving()) {
      event.preventDefault();
    } else {
      this.suggestionQueries.next(null);
    }
  }

  protected submit(): void {
    if (this.saving()) {
      return;
    }
    this.form.markAllAsTouched();
    if (this.form.invalid || this.form.controls.gameId.value === null) {
      return;
    }

    const value = this.form.getRawValue();
    this.saving.set(true);
    this.errorMessage.set(null);

    const request: SaveEntryRequest = {
      gameId: this.form.controls.gameId.value,
      score: value.score,
      status: value.status,
      ownership: value.ownership,
      playtime: value.playtime,
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

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Game } from '../models/game';
import { Platform } from '../models/platform';
import { GameSuggestion } from '../models/game-suggestion';
import { catchError, map, of, startWith, switchMap, timer } from 'rxjs';

export interface GameSuggestionsState {
  items: GameSuggestion[];
  loading: boolean;
  error: boolean;
}

@Injectable({ providedIn: 'root' })
export class GameService {
  private readonly http = inject(HttpClient);

  findById(id: number) {
    return this.http.get<Game>(`/api/games/${id}`);
  }

  platforms(id: number) {
    return this.http.get<Platform[]>(`/api/games/${id}/platforms`);
  }

  suggestions(name: string, size = 10) {
    return this.http.get<GameSuggestion[]>('/api/games/suggestions', {
      params: { name, limit: size },
    });
  }

  searchSuggestions(query: string | null, limit: number) {
    const name = query?.trim() ?? '';
    const empty: GameSuggestionsState = { items: [], loading: false, error: false };
    if (name.length < 3) return of(empty);

    // Call inside switchMap so new input cancels both the timer and the request.
    return timer(300).pipe(
      switchMap(() => this.suggestions(name, limit)),
      map((items) => ({ ...empty, items })),
      catchError(() => of({ ...empty, error: true })),
      startWith({ ...empty, loading: true }),
    );
  }
}

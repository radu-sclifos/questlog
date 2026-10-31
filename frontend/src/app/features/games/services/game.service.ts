import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { GameSuggestion } from '../models/game-suggestion';

@Injectable({ providedIn: 'root' })
export class GameService {
  private readonly http = inject(HttpClient);

  suggestions(name: string, size = 10) {
    return this.http.get<GameSuggestion[]>('/api/games/suggestions', {
      params: { name, limit: size },
    });
  }
}

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Game } from '../models/game';
import { Platform } from '../models/platform';
import { GameSuggestion } from '../models/game-suggestion';

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
}

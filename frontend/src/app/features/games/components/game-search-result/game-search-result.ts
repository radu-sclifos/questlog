import { Component, computed, input, linkedSignal } from '@angular/core';
import { GameSuggestion } from '../../models/game-suggestion';

@Component({
  selector: 'app-game-search-result',
  templateUrl: './game-search-result.html',
  host: { class: 'flex min-w-0 flex-1 items-center gap-3' },
})
export class GameSearchResult {
  readonly game = input.required<GameSuggestion>();

  protected readonly imageUrl = computed(() => {
    const id = this.game().coverImageId;
    return id
      ? `https://images.igdb.com/igdb/image/upload/t_cover_small/${encodeURIComponent(id)}.jpg`
      : null;
  });

  protected readonly imageFailed = linkedSignal({
    source: this.imageUrl,
    computation: () => false,
  });

  protected readonly year = computed(() => this.game().firstReleaseDate?.slice(0, 4));
}

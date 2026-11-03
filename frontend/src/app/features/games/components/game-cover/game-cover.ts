import { Component, computed, input, linkedSignal } from '@angular/core';

@Component({
  selector: 'app-game-cover',
  templateUrl: './game-cover.html',
})
export class GameCover {
  readonly imageId = input<string | null>(null);

  // Retry the cover when the component receives a different image
  protected readonly imageFailed = linkedSignal({
    source: this.imageId,
    computation: () => false,
  });

  protected readonly url = computed(() => {
    const id = this.imageId();
    if (id && !this.imageFailed()) {
      return `https://images.igdb.com/igdb/image/upload/t_cover_big/${encodeURIComponent(id)}.jpg`;
    } else {
      return null;
    }
  });
}

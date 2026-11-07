import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Subject, switchMap } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { GameService, GameSuggestionsState } from './game.service';

describe('GameService suggestion search', () => {
  let service: GameService;
  let http: HttpTestingController;
  let states: GameSuggestionsState[];

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(GameService);
    http = TestBed.inject(HttpTestingController);
    states = [];
  });

  afterEach(() => {
    try {
      http.verify();
    } finally {
      vi.useRealTimers();
    }
  });

  it.each([null, '', '   ', ' ab '])('does not request suggestions for %j', (query) => {
    service.searchSuggestions(query, 5).subscribe((state) => states.push(state));
    vi.advanceTimersByTime(300);
    http.expectNone((request) => request.url === '/api/games/suggestions');
    expect(states).toEqual([{ items: [], loading: false, error: false }]);
  });

  it('waits 300 ms, trims the query and returns suggestions with the requested limit', () => {
    service.searchSuggestions('  Zelda  ', 5).subscribe((state) => states.push(state));
    expect(states.at(-1)?.loading).toBe(true);
    vi.advanceTimersByTime(299);
    http.expectNone((request) => request.url === '/api/games/suggestions');
    vi.advanceTimersByTime(1);
    const request = http.expectOne('/api/games/suggestions?name=Zelda&limit=5');
    expect(request.request.method).toBe('GET');
    const games = [{ id: 42, name: 'Zelda' }];
    request.flush(games);
    expect(states.at(-1)).toEqual({ items: games, loading: false, error: false });
  });

  it('returns an error state and stops loading after an HTTP failure', () => {
    service.searchSuggestions('Zelda', 5).subscribe((state) => states.push(state));
    vi.advanceTimersByTime(300);
    http
      .expectOne('/api/games/suggestions?name=Zelda&limit=5')
      .flush(null, { status: 503, statusText: 'Service Unavailable' });
    expect(states.at(-1)).toEqual({ items: [], loading: false, error: true });
  });

  it('cancels the old debounce and in-flight request when the caller changes its query', () => {
    const queries = new Subject<string>();
    const subscription = queries
      .pipe(switchMap((query) => service.searchSuggestions(query, 5)))
      .subscribe((state) => states.push(state));
    queries.next('Mario');
    vi.advanceTimersByTime(200);
    queries.next('Zelda');
    vi.advanceTimersByTime(300);
    http.expectNone('/api/games/suggestions?name=Mario&limit=5');
    const outdated = http.expectOne('/api/games/suggestions?name=Zelda&limit=5');
    queries.next('Metroid');
    expect(outdated.cancelled).toBe(true);
    vi.advanceTimersByTime(300);
    const games = [{ id: 9, name: 'Metroid' }];
    http.expectOne('/api/games/suggestions?name=Metroid&limit=5').flush(games);
    expect(states.at(-1)?.items).toEqual(games);
    subscription.unsubscribe();
  });
});

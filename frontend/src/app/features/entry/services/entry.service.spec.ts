import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { EntryService } from './entry.service';
import { Entry, EntryStatus, SaveEntryRequest } from '../models/entry';
import { EntrySearchCriteria } from '../models/entry-search-criteria';
import { EntryPage } from '../models/entry-page';

describe('EntryService', () => {
  let service: EntryService;
  let http: HttpTestingController;
  const payload: SaveEntryRequest = {
    gameId: 42,
    platformId: null,
    score: 8.5,
    notes: 'A memorable journey',
    status: null,
    ownership: null,
    playtime: 12,
    achievementsUnlocked: 2,
    achievementsTotal: 10,
  };
  const saved: Entry = {
    ...payload,
    id: 7,
    game: { id: 42, name: 'Zelda', coverImageId: null },
    platform: null,
    status: EntryStatus.UNPLAYED,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EntryService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('posts library search criteria and returns the server page', async () => {
    const criteria = new EntrySearchCriteria('player');
    const result = firstValueFrom(service.search(criteria));
    const request = http.expectOne('/api/entries/search');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(criteria);
    const page: EntryPage = { items: [saved], page: 0, size: 25, totalElements: 1, totalPages: 1 };
    request.flush(page);
    expect(await result).toEqual(page);
  });

  it('posts the selected game and personal fields when creating an entry', async () => {
    const result = firstValueFrom(service.create(payload));
    const request = http.expectOne('/api/entries');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush(saved);
    expect(await result).toEqual(saved);
  });

  it('puts changes to the existing entry endpoint', async () => {
    const result = firstValueFrom(service.update(7, payload));
    const request = http.expectOne('/api/entries/7');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(payload);
    request.flush(saved);
    expect(await result).toEqual(saved);
  });

  it('propagates a duplicate-game conflict to the caller', async () => {
    const result = firstValueFrom(service.create(payload));
    const rejected = expect(result).rejects.toMatchObject({ status: 409 });
    http.expectOne('/api/entries').flush(null, { status: 409, statusText: 'Conflict' });
    await rejected;
  });
});

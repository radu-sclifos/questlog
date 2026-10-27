import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Entry, SaveEntryRequest } from '../models/entry';
import { EntrySearchCriteria } from '../models/entry-search-criteria';
import { EntryPage } from '../models/entry-page';

@Injectable({ providedIn: 'root' })
export class EntryService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/entries';

  search(criteria: EntrySearchCriteria): Observable<EntryPage> {
    return this.http.post<EntryPage>(`${this.url}/search`, criteria);
  }

  findById(id: number): Observable<Entry> {
    return this.http.get<Entry>(`${this.url}/${id}`);
  }

  create(request: SaveEntryRequest): Observable<Entry> {
    return this.http.post<Entry>(this.url, request);
  }

  update(id: number, request: SaveEntryRequest): Observable<Entry> {
    return this.http.put<Entry>(`${this.url}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}

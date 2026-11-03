import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { PublicProfile } from '../models/public-profile';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);

  get(username: string) {
    return this.http.get<PublicProfile>(`/api/users/${encodeURIComponent(username)}/profile`);
  }

  saveFavorites(gameIds: number[]) {
    return this.http.put<void>('/api/me/favorites', { gameIds });
  }

  uploadAvatar(file: File) {
    const data = new FormData();
    data.append('file', file);
    return this.http.put<{ revision: string }>('/api/me/avatar', data);
  }

  removeAvatar() {
    return this.http.delete<void>('/api/me/avatar');
  }
}

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { PublicProfile } from '../models/public-profile';
import { catchError, filter, map, merge, of, startWith, Subject, tap } from 'rxjs';
import { AuthService } from '../../auth/services/auth.service';
import { avatarUrl } from '../utils/profile-image';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly avatarUpdates = new Subject<{ username: string; revision: string | null }>();

  get(username: string) {
    return this.http.get<PublicProfile>(`/api/users/${encodeURIComponent(username)}/profile`);
  }

  watchAvatar(username: string) {
    return merge(
      this.get(username).pipe(
        map((profile) => profile.avatarRevision),
        catchError(() => of(null)),
      ),
      this.avatarUpdates.pipe(
        filter((change) => change.username === username),
        map((change) => change.revision),
      ),
    ).pipe(
      map((revision) => avatarUrl(username, revision)),
      startWith(null),
    );
  }

  saveFavorites(gameIds: number[]) {
    return this.http.put<void>('/api/me/favorites', { gameIds });
  }

  uploadAvatar(file: File) {
    const data = new FormData();
    data.append('file', file);
    const username = this.auth.user()?.username;
    return this.http.put<{ revision: string }>('/api/me/avatar', data).pipe(
      tap(({ revision }) => {
        if (username) {
          this.avatarUpdates.next({ username, revision });
        }
      }),
    );
  }

  removeAvatar() {
    const username = this.auth.user()?.username;
    return this.http.delete<void>('/api/me/avatar').pipe(
      tap(() => {
        if (username) {
          this.avatarUpdates.next({ username, revision: null });
        }
      }),
    );
  }
}

import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { switchMap, tap } from 'rxjs';

export interface UserProfile {
  username: string;
  displayName: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly currentUser = signal<UserProfile | null>(null);
  readonly user = this.currentUser.asReadonly();

  restoreSession() {
    return this.http.get<UserProfile>('/api/me').pipe(tap((user) => this.currentUser.set(user)));
  }

  login(username: string, password: string) {
    const body = new HttpParams().set('username', username).set('password', password);
    return this.http.post<void>('/api/auth/login', body)
      .pipe(switchMap(() => this.restoreSession()));
  }

  logout() {
    return this.http.post<void>('/api/auth/logout', null).pipe(tap(() => this.clearSession()));
  }

  clearSession(): void {
    this.currentUser.set(null);
  }
}

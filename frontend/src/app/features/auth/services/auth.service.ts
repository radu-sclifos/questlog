import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { catchError, Observable, of, shareReplay, switchMap, tap, timeout } from 'rxjs';

export interface UserProfile {
  username: string;
  displayName: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly currentUser = signal<UserProfile | null>(null);
  public readonly user = this.currentUser.asReadonly();

  private readonly unavailable = signal(false);
  public readonly sessionUnavailable = this.unavailable.asReadonly();

  private initialization$?: Observable<UserProfile | null>;

  /** Shared startup check; navigation and components reuse the stored profile. */
  initializeSession(): Observable<UserProfile | null> {
    return (this.initialization$ ??= this.loadProfile().pipe(
      timeout(8_000),
      catchError((error: unknown) => {
        this.clearSession();
        this.unavailable.set(!(error instanceof HttpErrorResponse && error.status === 401));
        return of(null);
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    ));
  }

  private loadProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>('/api/me').pipe(
      tap((user) => {
        this.currentUser.set(user);
        this.unavailable.set(false);
      }),
    );
  }

  login(username: string, password: string): Observable<UserProfile> {
    const body = new HttpParams().set('username', username).set('password', password);
    return this.http.post<void>('/api/auth/login', body)
      .pipe(switchMap(() => this.loadProfile()));
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', null)
      .pipe(tap(() => this.clearSession()));
  }

  clearSession(): void {
    this.currentUser.set(null);
    this.unavailable.set(false);
  }
}

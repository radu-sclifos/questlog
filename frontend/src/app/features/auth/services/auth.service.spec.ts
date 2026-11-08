import { HttpParams, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;
  const user = { username: 'player', displayName: 'Player' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    try {
      http.verify();
    } finally {
      vi.useRealTimers();
    }
  });

  it('shares the startup request and caches the profile for subsequent subscribers', async () => {
    const first = firstValueFrom(service.initializeSession());
    const second = firstValueFrom(service.initializeSession());
    const request = http.expectOne('/api/me');
    expect(request.request.method).toBe('GET');
    request.flush(user);
    expect(await first).toEqual(user);
    expect(await second).toEqual(user);
    expect(service.user()).toEqual(user);
    expect(service.sessionUnavailable()).toBe(false);
    expect(await firstValueFrom(service.initializeSession())).toEqual(user);
    http.expectNone('/api/me');
  });

  it.each([
    { status: 401, unavailable: false },
    { status: 503, unavailable: true },
  ])('handles HTTP $status with unavailable=$unavailable', async ({ status, unavailable }) => {
    const result = firstValueFrom(service.initializeSession());
    http.expectOne('/api/me').flush(null, { status, statusText: 'Session check failed' });
    expect(await result).toBeNull();
    expect(service.user()).toBeNull();
    expect(service.sessionUnavailable()).toBe(unavailable);
  });

  it('marks a timed-out session check as unavailable after eight seconds', async () => {
    vi.useFakeTimers();
    const result = firstValueFrom(service.initializeSession());
    const request = http.expectOne('/api/me');
    await vi.advanceTimersByTimeAsync(8_000);
    expect(await result).toBeNull();
    expect(request.cancelled).toBe(true);
    expect(service.sessionUnavailable()).toBe(true);
  });

  it('sends form-encoded login fields and loads the authenticated profile', async () => {
    const result = firstValueFrom(service.login('player', 'test-only-password'));
    const login = http.expectOne('/api/auth/login');
    expect(login.request.method).toBe('POST');
    expect(login.request.body).toBeInstanceOf(HttpParams);
    expect(login.request.body.get('username')).toBe('player');
    expect(login.request.body.get('password')).toBe('test-only-password');
    login.flush(null);
    http.expectOne('/api/me').flush(user);
    expect(await result).toEqual(user);
    expect(service.user()).toEqual(user);
  });

  it('clears the local profile after a successful logout', async () => {
    const initialization = firstValueFrom(service.initializeSession());
    http.expectOne('/api/me').flush(user);
    await initialization;
    const result = firstValueFrom(service.logout());
    const logout = http.expectOne('/api/auth/logout');
    expect(logout.request.method).toBe('POST');
    logout.flush(null);
    await result;
    expect(service.user()).toBeNull();
    expect(service.sessionUnavailable()).toBe(false);
  });
});

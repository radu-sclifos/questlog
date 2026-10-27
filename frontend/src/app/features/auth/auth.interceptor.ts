import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { switchMap, tap } from 'rxjs';
import { AuthService } from './services/auth.service';

interface CsrfToken {
  headerName: string;
  token: string;
}

export const authInterceptor: HttpInterceptorFn = (request, next) => {

  if (!request.url.startsWith('/api/')) {
    return next(request);
  }

  const httpClient = inject(HttpClient);
  const authService = inject(AuthService);
  const router = inject(Router);
  const publicSearch = request.method === 'POST' && request.url === '/api/entries/search';
  const safeMethod = ['GET', 'HEAD', 'OPTIONS'].includes(request.method);
  // obtain a fresh token for each mutation including after login/logout rotate it.
  const response = safeMethod || publicSearch
    ? next(request)
    : httpClient
        .get<CsrfToken>('/api/auth/csrf')
        .pipe(
          switchMap((csrf) =>
            next(request.clone({ setHeaders: { [csrf.headerName]: csrf.token } })),
          ),
        );

  return response.pipe(
    tap({
      error: (error: unknown) => {
        if (
          error instanceof HttpErrorResponse &&
          error.status === 401 &&
          !publicSearch &&
          request.url !== '/api/auth/login' &&
          request.url !== '/api/me'
        ) {
          authService.clearSession();
          void router.navigateByUrl('/login');
        }
      },
    }),
  );

};

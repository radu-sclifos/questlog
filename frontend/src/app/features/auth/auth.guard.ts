import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './services/auth.service';

export const authGuard: CanActivateFn = () => {

  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.restoreSession().pipe(
    map(() => true),
    catchError((error: unknown) => {
      auth.clearSession();
      return of(
        router.createUrlTree(['/login'], {
          queryParams:
            error instanceof HttpErrorResponse && error.status === 401 ? {} : { reason: 'unavailable' },
        }),
      );
    }),
  );

};

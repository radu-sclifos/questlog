import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const homeRedirectGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const user = auth.user();
  return user
    ? router.createUrlTree(['/list', user.username])
    : router.createUrlTree(['/login'], {
        queryParams: auth.sessionUnavailable() ? { reason: 'unavailable' } : {},
      });
};

import { Component, inject, output, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../auth/services/auth.service';
import { ToastService } from '../../../../shared/toast/toast.service';

@Component({
  selector: 'app-entry-navbar',
  templateUrl: './entry-navbar.html',
  styleUrl: './entry-navbar.scss',
})
export class EntryNavbar {
  readonly addEntry = output<void>();
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  protected readonly signingOut = signal(false);

  protected logout(): void {
    if (this.signingOut()) return;
    this.signingOut.set(true);
    this.auth.logout().pipe(finalize(() => this.signingOut.set(false))).subscribe({
      next: () => { void this.router.navigateByUrl('/login'); },
      error: () => this.toast.error('Unable to sign out. Please try again.'),
    });
  }
}

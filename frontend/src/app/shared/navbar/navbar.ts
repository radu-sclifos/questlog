import {
  Component,
  DestroyRef,
  ElementRef,
  HostListener,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter, finalize } from 'rxjs';
import { AuthService } from '../../features/auth/services/auth.service';
import { initials } from '../../features/profile/utils/profile-image';
import { Logo } from '../logo/logo';
import { ToastService } from '../toast/toast.service';

@Component({
  selector: 'app-navbar',
  imports: [Logo, RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly profileMenu = viewChild<ElementRef<HTMLDetailsElement>>('profileMenu');
  protected readonly user = this.auth.user;
  protected readonly signingOut = signal(false);
  protected readonly initials = initials;

  constructor() {
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.closeMenu());
  }

  @HostListener('document:click', ['$event'])
  protected onDocumentClick(event: MouseEvent): void {
    const menu = this.profileMenu()?.nativeElement;
    if (menu && event.target instanceof Node && !menu.contains(event.target)) this.closeMenu();
  }

  @HostListener('document:keydown.escape')
  protected closeMenu(): void {
    const menu = this.profileMenu()?.nativeElement;
    if (menu?.open) {
      if (menu.contains(document.activeElement)) menu.querySelector('summary')?.focus();
      menu.open = false;
    }
  }

  protected logout(): void {
    if (this.signingOut()) return;
    this.signingOut.set(true);
    this.auth
      .logout()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.signingOut.set(false)),
      )
      .subscribe({
        next: () => {
          void this.router.navigateByUrl('/login');
        },
        error: () => this.toast.error('Unable to sign out. Please try again.'),
      });
  }

  private readonly destroyRef = inject(DestroyRef);
}

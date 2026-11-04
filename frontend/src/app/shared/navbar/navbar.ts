import {
  Component,
  computed,
  DestroyRef,
  ElementRef,
  HostListener,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { distinctUntilChanged, filter, finalize, map, of, switchMap } from 'rxjs';
import { AuthService } from '../../features/auth/services/auth.service';
import { initials } from '../../features/profile/utils/profile-image';
import { ProfileService } from '../../features/profile/services/profile.service';
import { Logo } from '../logo/logo';
import { NavbarSearch } from './components/navbar-search/navbar-search';
import { ToastService } from '../toast/toast.service';

@Component({
  selector: 'app-navbar',
  imports: [Logo, RouterLink, NavbarSearch],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar {
  private readonly auth = inject(AuthService);
  private readonly profiles = inject(ProfileService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly initials = initials;
  private readonly profileMenu = viewChild<ElementRef<HTMLDetailsElement>>('profileMenu');

  protected readonly user = this.auth.user;
  protected readonly signingOut = signal(false);
  private readonly failedAvatar = signal<string | null>(null);
  private readonly avatar = toSignal(
    toObservable(this.user).pipe(
      map((user) => user?.username ?? null),
      distinctUntilChanged(),
      switchMap((username) => (username ? this.profiles.watchAvatar(username) : of(null))),
    ),
    { initialValue: null },
  );
  protected readonly visibleAvatar = computed(() => {
    if (this.avatar() !== this.failedAvatar()) {
      return this.avatar();
    } else {
      return null;
    }
  });

  constructor() {
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.closeMenu());
  }

  protected onAvatarError(url: string): void {
    this.failedAvatar.set(url);
  }

  @HostListener('document:click', ['$event'])
  protected onDocumentClick(event: MouseEvent): void {
    const menu = this.profileMenu()?.nativeElement;
    if (menu && event.target instanceof Node && !menu.contains(event.target)) {
      this.closeMenu();
    }
  }

  @HostListener('document:keydown.escape')
  protected closeMenu(): void {
    const menu = this.profileMenu()?.nativeElement;
    if (menu?.open) {
      if (menu.contains(document.activeElement)) {
        menu.querySelector('summary')?.focus();
      }
      menu.open = false;
    }
  }

  protected logout(): void {
    if (this.signingOut()) {
      return;
    }
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

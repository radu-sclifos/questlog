import { HttpClient } from '@angular/common/http';
import { Component, computed, ElementRef, HostListener, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { catchError, filter, map, of, startWith, switchMap, timer } from 'rxjs';
import { GameService } from '../../../../features/games/services/game.service';
import { GameSuggestion } from '../../../../features/games/models/game-suggestion';
import { GameSearchResult } from '../../../../features/games/components/game-search-result/game-search-result';
import { avatarUrl, initials } from '../../../../features/profile/utils/profile-image';

interface UserSuggestion {
  username: string;
  displayName: string;
  avatarRevision: string | null;
}

interface SearchResult {
  game?: GameSuggestion;
  path: string[];
  title: string;
  subtitle: string;
  image: string | null;
}

const emptyResults = { items: [] as SearchResult[], loading: false, error: false };

@Component({
  selector: 'app-navbar-search',
  imports: [RouterLink, GameSearchResult],
  templateUrl: './navbar-search.html',
  host: { class: 'block min-w-0' },
})
export class NavbarSearch {
  private readonly games = inject(GameService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly initials = initials;

  protected readonly query = signal('');
  protected readonly mode = signal<'games' | 'users'>('games');
  protected readonly open = signal(false);
  protected readonly failedImages = signal(new Set<string>());

  private readonly criteria = computed(() => ({ name: this.query().trim(), mode: this.mode() }));

  protected readonly results = toSignal(
    toObservable(this.criteria).pipe(
      switchMap(({ name, mode }) => {
        if (name.length < 3) {
          return of(emptyResults);
        }
        if (mode === 'games') {
          return this.games.searchSuggestions(name, 6).pipe(
            map((state) => ({
              ...state,
              items: state.items.map((game): SearchResult => ({
                game,
                path: ['/game', String(game.id)],
                title: game.name,
                subtitle: '',
                image: null,
              })),
            })),
          );
        } else {
          const request = this.http
            .get<UserSuggestion[]>('/api/users/suggestions', { params: { name, limit: 6 } })
            .pipe(
              map((users) =>
                users.map((user): SearchResult => ({
                  path: ['/user', user.username],
                  title: user.displayName || user.username,
                  subtitle: `@${user.username}`,
                  image: avatarUrl(user.username, user.avatarRevision),
                })),
              ),
            );
          return timer(300).pipe(
            switchMap(() => request),
            map((items) => ({ items, loading: false, error: false })),
            catchError(() => of({ ...emptyResults, error: true })),
            startWith({ ...emptyResults, loading: true }),
          );
        }
      }),
    ),
    { initialValue: emptyResults },
  );

  constructor() {
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        this.open.set(false);
        this.query.set('');
      });
  }

  protected setMode(mode: 'games' | 'users'): void {
    this.mode.set(mode);
    this.open.set(true);
    this.focusInput();
  }

  protected imageFailed(url: string): void {
    this.failedImages.update((images) => new Set([...images, url]));
  }

  protected onFocusOut(event: FocusEvent): void {
    if (
      !(event.relatedTarget instanceof Node) ||
      !this.element.nativeElement.contains(event.relatedTarget)
    ) {
      this.open.set(false);
    }
  }

  @HostListener('document:click', ['$event'])
  protected onOutsideClick(event: MouseEvent): void {
    if (event.target instanceof Node && !this.element.nativeElement.contains(event.target))
      this.open.set(false);
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.open.set(false);
      this.focusInput();
      // Focusing the input can reopen the panel
      this.open.set(false);
      return;
    }

    if (event.key !== 'ArrowDown' && event.key !== 'ArrowUp' && event.key !== 'Enter') return;
    const links = Array.from(
      this.element.nativeElement.querySelectorAll<HTMLAnchorElement>('[data-search-result]'),
    );

    const index = links.findIndex((link) => link === document.activeElement);
    if (event.key === 'Enter') {
      if (event.target instanceof HTMLInputElement && links.length) {
        event.preventDefault();
        links[0].click();
      }
      return;
    }

    event.preventDefault();
    this.open.set(true);
    if (event.key === 'ArrowUp' && index <= 0) {
      this.focusInput();
    } else {
      links[event.key === 'ArrowDown' ? Math.min(index + 1, links.length - 1) : index - 1]?.focus();
    }
  }

  private focusInput(): void {
    this.element.nativeElement.querySelector('input')?.focus();
  }
}

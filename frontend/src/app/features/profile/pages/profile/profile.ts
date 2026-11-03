import { DatePipe, DecimalPipe, ViewportScroller } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  catchError,
  distinctUntilChanged,
  EMPTY,
  finalize,
  map,
  Subject,
  switchMap,
  tap,
} from 'rxjs';
import { AuthService } from '../../../auth/services/auth.service';
import { EntryNavbar } from '../../../entry/components/entry-navbar/entry-navbar';
import { EntryStatus } from '../../../entry/models/entry';
import { ENTRY_STATUS_LABELS } from '../../../entry/models/entry-labels';
import { GameCover } from '../../../games/components/game-cover/game-cover';
import { Logo } from '../../../../shared/logo/logo';
import { PublicProfile } from '../../models/public-profile';
import { avatarUrl, initials } from '../../utils/profile-image';
import { ProfileService } from '../../services/profile.service';
import { ProfileEditor } from '../../components/profile-editor/profile-editor';
import { FavoritesEditor } from '../../components/favorites-editor/favorites-editor';

const STATUS_COLORS: Record<EntryStatus, string> = {
  PLAYING: '#38bdf8',
  BEATEN: '#22c55e',
  COMPLETED: '#a78bfa',
  UNPLAYED: '#94a3b8',
  ON_HOLD: '#fba524',
  ENDLESS: '#fdf5bf',
  DROPPED: '#d35e6d',
};

@Component({
  selector: 'app-profile',
  imports: [
    RouterLink,
    DatePipe,
    DecimalPipe,
    EntryNavbar,
    GameCover,
    Logo,
    ProfileEditor,
    FavoritesEditor,
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile {
  private readonly profiles = inject(ProfileService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly title = inject(Title);
  private readonly viewport = inject(ViewportScroller);

  private readonly requests = new Subject<string>();

  protected readonly username = signal('');
  protected readonly profile = signal<PublicProfile | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly imageFailed = signal(false);

  protected readonly canEdit = computed(
    () => !!this.profile() && this.auth.user()?.username === this.username(),
  );
  protected readonly avatar = computed(() =>
    avatarUrl(this.username(), this.profile()?.avatarRevision ?? null),
  );

  protected readonly initials = initials;
  protected readonly EntryStatus = EntryStatus;
  protected readonly statuses = computed(() => {
    const counts = this.profile()?.statistics.statuses;
    return Object.entries(STATUS_COLORS).map(([status, color]) => ({
      status: status as EntryStatus,
      color,
      label: ENTRY_STATUS_LABELS[status as EntryStatus],
      count: counts?.[status as EntryStatus] ?? 0,
    }));
  });
  protected readonly donut = computed(() => {
    const total = this.profile()?.statistics.totalGames ?? 0;
    if (!total) {
      return 'var(--color-base-300)';
    }
    let start = 0;
    return `conic-gradient(${this.statuses()
      .filter((item) => item.count)
      .map((item) => {
        const end = start + (item.count / total) * 100;
        const segment = `${item.color} ${start}% ${end}%`;
        start = end;
        return segment;
      })
      .join(', ')})`;
  });

  constructor() {
    this.requests
      .pipe(
        switchMap((username) => {
          this.loading.set(true);
          this.error.set(null);

          return this.profiles.get(username).pipe(
            catchError((error: unknown) => {
              this.error.set(
                error instanceof HttpErrorResponse && error.status === 404
                  ? 'User not found.'
                  : 'Unable to load this profile. Please try again later.',
              );
              return EMPTY;
            }),
            finalize(() => this.loading.set(false)),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((profile) => {
        this.profile.set(profile);
        this.title.setTitle(`${profile.displayName} - QuestLog`);
      });

    this.route.paramMap
      .pipe(
        map((params) => params.get('username') ?? ''),
        distinctUntilChanged(),
        tap(() => this.viewport.scrollToPosition([0, 0])),
        takeUntilDestroyed(),
      )
      .subscribe((username) => {
        this.username.set(username);
        this.profile.set(null);
        this.imageFailed.set(false);
        this.title.setTitle('Profile - QuestLog');
        this.requests.next(username);
      });
  }

  protected reload(): void {
    this.requests.next(this.username());
  }

  protected updateAvatar(revision: string | null): void {
    this.profile.update((profile) => (profile ? { ...profile, avatarRevision: revision } : null));
    this.imageFailed.set(false);
  }
}

import { DatePipe, ViewportScroller } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { catchError, distinctUntilChanged, EMPTY, finalize, map, switchMap } from 'rxjs';
import { GameService } from '../../services/game.service';
import { Game, GameRelationType } from '../../models/game';
import { CompanyRole } from '../../../companies/models/company';
import { sortPlatforms } from '../../models/platform-order';
import { GameCover } from '../../components/game-cover/game-cover';
import { ExpandableText } from '../../components/expandable-text/expandable-text';
import { GameJourney } from '../../components/game-journey/game-journey';

const RELATION_LABELS: Record<GameRelationType, string> = {
  PARENT_GAME: 'Base game',
  VERSION_OF: 'Original game',
  DLC: 'DLC',
  EXPANSION: 'Expansions',
  STANDALONE_EXPANSION: 'Standalone expansions',
  REMAKE: 'Remakes',
  REMASTER: 'Remasters',
  PORT: 'Ports',
  EXPANDED_GAME: 'Expanded games',
  FORK: 'Forks',
};

@Component({
  selector: 'app-game-detail',
  imports: [DatePipe, RouterLink, GameCover, GameJourney, ExpandableText],
  templateUrl: './game-detail.html',
  styleUrl: './game-detail.scss',
})
export class GameDetail {
  private readonly games = inject(GameService);
  private readonly route = inject(ActivatedRoute);
  private readonly title = inject(Title);
  private readonly viewportScroller = inject(ViewportScroller);

  protected readonly game = signal<Game | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly notFound = signal(false);
  protected readonly developers = computed(() => this.companyLinks('developer'));
  protected readonly detailFields = computed(() => {
    const game = this.game();
    if (!game) {
      return [];
    }
    const porting = this.companyLinks('porting');
    const supporting = this.companyLinks('supporting');
    return [
      { label: 'Developer', value: this.developers() },
      { label: 'Publisher', value: this.companyLinks('publisher') },
      ...(porting.length || supporting.length
        ? [
            { label: 'Porting', value: porting },
            { label: 'Supporting studios', value: supporting },
          ]
        : []),
      { label: 'Series', value: this.names(game.series) },
      { label: 'Franchise', value: this.names(game.franchises) },
      { label: 'Platforms', value: this.names(sortPlatforms(game.platforms)) },
      { label: 'Engine', value: this.names(game.engines) },
    ];
  });
  protected readonly relationGroups = computed(() => {
    const relations = this.game()?.relations ?? [];

    return (Object.entries(RELATION_LABELS) as [GameRelationType, string][])
      .map(([type, label]) => ({
        type,
        label,
        games: relations
          .filter((relation) => relation.relationType === type)
          .map((relation) => relation.game)
          .sort((a, b) => a.name.localeCompare(b.name)),
      }))
      .filter((group) => group.games.length);
  });

  constructor() {
    this.route.paramMap
      .pipe(
        map((params) => params.get('id') ?? ''),
        distinctUntilChanged(),
        switchMap((id) => this.loadGame(id)),
        takeUntilDestroyed(),
      )
      .subscribe((game) => {
        this.game.set(game);
        this.title.setTitle(`${game.name} - QuestLog`);
      });
  }

  private loadGame(id: string) {
    this.viewportScroller.scrollToPosition([0, 0]);
    this.game.set(null);
    this.error.set(null);
    this.notFound.set(false);
    this.title.setTitle('Game - QuestLog');
    const gameId = Number(id);

    if (!/^\d+$/.test(id) || !Number.isSafeInteger(gameId) || gameId < 1) {
      this.notFound.set(true);
      this.loading.set(false);
      return EMPTY;
    }

    this.loading.set(true);
    return this.games.findById(gameId).pipe(
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 404) {
          this.notFound.set(true);
        } else {
          this.error.set('Unable to load this game.');
        }
        return EMPTY;
      }),
      finalize(() => this.loading.set(false)),
    );
  }

  private names(values: readonly { name: string }[]): string[] {
    return values.map((value) => value.name);
  }

  private companyLinks(role: CompanyRole) {
    return (this.game()?.involvedCompanies ?? [])
      .filter((company) => company[role])
      .map(({ company }) => ({
        name: company.name,
        route: ['/company', company.id],
      }));
  }
}

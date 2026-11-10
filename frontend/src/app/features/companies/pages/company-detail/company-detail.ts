import { ViewportScroller } from '@angular/common';
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
  startWith,
  Subject,
  switchMap,
} from 'rxjs';
import { GameCover } from '../../../games/components/game-cover/game-cover';
import { GameSummary } from '../../../games/models/game';
import { Company, CompanyRole } from '../../models/company';
import { CompanyService } from '../../services/company.service';

@Component({
  selector: 'app-company-detail',
  imports: [RouterLink, GameCover],
  templateUrl: './company-detail.html',
})
export class CompanyDetail {
  private readonly companies = inject(CompanyService);
  private readonly route = inject(ActivatedRoute);
  private readonly title = inject(Title);
  private readonly viewportScroller = inject(ViewportScroller);
  private readonly roleSelection = new Subject<CompanyRole>();

  protected readonly roleLabels: Record<CompanyRole, string> = {
    developer: 'Developed',
    publisher: 'Published',
    porting: 'Ported',
    supporting: 'Supported',
  };

  protected readonly company = signal<Company | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly notFound = signal(false);
  protected readonly logoFailed = signal(false);
  protected readonly logoUrl = computed(() => {
    const id = this.company()?.logoImageId;
    return id && !this.logoFailed()
      ? `https://images.igdb.com/igdb/image/upload/t_original/${encodeURIComponent(id)}.png`
      : null;
  });
  protected readonly selectedRole = signal<CompanyRole | null>(null);
  protected readonly games = signal<GameSummary[]>([]);
  protected readonly gamesLoading = signal(false);
  protected readonly gamesError = signal(false);

  constructor() {
    this.route.paramMap
      .pipe(
        map((params) => params.get('id') ?? ''),
        distinctUntilChanged(),
        switchMap((id) =>
          this.loadCompany(id).pipe(
            switchMap((company) => {
              this.company.set(company);
              this.title.setTitle(`${company.name} - QuestLog`);
              const [firstRole] = company.roles;
              if (!firstRole) {
                return EMPTY;
              }

              return this.roleSelection.pipe(
                startWith(firstRole),
                distinctUntilChanged(),
                switchMap((role) => this.loadGames(company.id, role)),
              );
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((games) => this.games.set(games));
  }

  protected selectRole(role: CompanyRole): void {
    this.roleSelection.next(role);
  }

  private loadCompany(id: string) {
    this.viewportScroller.scrollToPosition([0, 0]);
    this.title.setTitle('Company - QuestLog');
    this.company.set(null);
    this.error.set(null);
    this.notFound.set(false);
    this.logoFailed.set(false);
    this.selectedRole.set(null);
    this.games.set([]);
    this.gamesError.set(false);

    const companyId = Number(id);
    if (!/^\d+$/.test(id) || !Number.isSafeInteger(companyId) || companyId < 1) {
      this.notFound.set(true);
      this.loading.set(false);
      return EMPTY;
    }

    this.loading.set(true);
    return this.companies.findById(companyId).pipe(
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 404) {
          this.notFound.set(true);
        } else {
          this.error.set('Unable to load this company.');
        }
        return EMPTY;
      }),
      finalize(() => this.loading.set(false)),
    );
  }

  private loadGames(id: number, role: CompanyRole) {
    this.selectedRole.set(role);
    this.games.set([]);
    this.gamesError.set(false);
    this.gamesLoading.set(true);

    return this.companies.findGames(id, role).pipe(
      catchError(() => {
        this.gamesError.set(true);
        return EMPTY;
      }),
      finalize(() => this.gamesLoading.set(false)),
    );
  }
}

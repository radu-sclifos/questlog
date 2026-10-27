import { EntryListHeader } from '../../components/entry-list-header/entry-list-header';
import { Component, computed, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, distinctUntilChanged, EMPTY, finalize, map, Subject, switchMap } from 'rxjs';
import { AuthService } from '../../../auth/services/auth.service';
import { EntryCard } from '../../components/entry-card/entry-card';
import { EntryNavbar } from '../../components/entry-navbar/entry-navbar';
import { EntryEditorDialog } from '../../components/entry-editor-dialog/entry-editor-dialog';
import { EntryDeleteDialog } from '../../components/entry-delete-dialog/entry-delete-dialog';
import { EntryService } from '../../services/entry.service';
import { Entry } from '../../models/entry';
import { EntryPage } from '../../models/entry-page';
import { ToastService } from '../../../../shared/toast/toast.service';
import { EntrySearchCriteria } from '../../models/entry-search-criteria';
import { SearchCriteria } from '../../../../shared/search/search-criteria';

@Component({
  selector: 'app-entry-list',
  imports: [EntryListHeader, EntryCard, EntryNavbar, EntryEditorDialog, EntryDeleteDialog],
  templateUrl: './entry-list.html',
  styleUrl: './entry-list.scss',
})
export class EntryList {
  private readonly entryService = inject(EntryService);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  private readonly searchRequested = new Subject<EntrySearchCriteria>();
  private currentCriteria?: EntrySearchCriteria;
  private readonly defaultPageSize = 10;

  protected readonly username = signal('');
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly page = signal<EntryPage | null>(null);

  protected readonly canEdit = computed(() => this.auth.user()?.username === this.username());

  private readonly listStart = viewChild<ElementRef<HTMLElement>>('listStart');
  private readonly paginationControls = viewChild<ElementRef<HTMLElement>>('paginationControls');

  constructor() {
    effect((onCleanup) => {
      const pagSidebar = this.paginationControls()?.nativeElement;
      if (!pagSidebar) {
        return;
      }

      // Keep the sticky offset centered even when the controls change height
      const observer = new ResizeObserver(() => {
        pagSidebar.style.setProperty('--pagination-height', `${pagSidebar.offsetHeight}px`);
      });
      observer.observe(pagSidebar);
      onCleanup(() => observer.disconnect());
    });

    // Listen before the route emits the initial search criteria
    this.searchRequested
      .pipe(
        switchMap((criteria) => {
          this.errorMessage.set(null);
          this.loading.set(true);

          return this.entryService.search(criteria).pipe(
            catchError((error: unknown) => {
              this.errorMessage.set('Unable to load this library.');
              console.error('Error during load entries list:', error);
              return EMPTY;
            }),
            finalize(() => this.loading.set(false)),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((page) => {
        // Deleting the last item on a page can move the last available page back.
        if (page.page > 0 && page.page >= page.totalPages && this.currentCriteria) {
          this.requestPage(Math.max(0, page.totalPages - 1));
          return;
        }
        const previousPage = this.page()?.page;
        this.page.set(page);
        if (previousPage !== undefined && previousPage !== page.page) {
          this.listStart()?.nativeElement.scrollIntoView({ block: 'start', behavior: 'instant' });
        }
      });

    this.route.paramMap
      .pipe(
        map((params) => params.get('username') ?? ''),
        distinctUntilChanged(),
        takeUntilDestroyed(),
      )
      .subscribe((username) => {
        this.username.set(username);
        this.page.set(null);
        this.onCriteriaChange(new SearchCriteria());
      });
  }

  protected onCriteriaChange(criteria: SearchCriteria): void {
    this.currentCriteria = Object.assign(new EntrySearchCriteria(this.username()), criteria);
    this.currentCriteria.pageNumber = 0;
    this.currentCriteria.pageSize = this.page()?.size ?? this.defaultPageSize;
    this.searchRequested.next(this.currentCriteria);
  }

  protected onEntrySaved(entry: Entry): void {
    const isUpdate = this.page()?.items.some((existing) => existing.id === entry.id);
    this.toast.success(isUpdate ? 'Game updated.' : 'Game added.');
    this.reloadLibrary();
  }

  protected onEntryDeleted(): void {
    this.toast.success('Game deleted.');
    this.reloadLibrary();
  }

  private reloadLibrary(): void {
    if (this.currentCriteria) {
      this.searchRequested.next(this.currentCriteria);
    }
  }

  protected onPageChange(page: number): void {
    const result = this.page();
    if (
      !result ||
      this.loading() ||
      !Number.isInteger(page) ||
      page < 0 ||
      page >= result.totalPages ||
      page === result.page
    ) {
      return;
    }
    this.requestPage(page);
  }

  private requestPage(page: number): void {
    if (!this.currentCriteria) return;
    this.currentCriteria = Object.assign(
      new EntrySearchCriteria(this.username()),
      this.currentCriteria,
      { pageNumber: page },
    );
    this.searchRequested.next(this.currentCriteria);
  }

  protected readonly firstResult = computed(() => {
    const result = this.page();
    return result?.totalElements ? result.page * result.size + 1 : 0;
  });
  protected readonly lastResult = computed(() => {
    const result = this.page();
    return result ? Math.min((result.page + 1) * result.size, result.totalElements) : 0;
  });
  protected readonly visiblePages = computed(() => {
    const result = this.page();
    if (!result || result.totalPages === 0) return [];
    const count = Math.min(3, result.totalPages);
    const start = Math.max(0, Math.min(result.page - 1, result.totalPages - count));
    return Array.from({ length: count }, (_, index) => start + index);
  });

  protected readonly pageOptions = computed(() =>
    Array.from({ length: this.page()?.totalPages ?? 0 }, (_, index) => index),
  );
}

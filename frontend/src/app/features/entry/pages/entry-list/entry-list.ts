import { EntryListHeader } from '../../components/entry-list-header/entry-list-header';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, distinctUntilChanged, EMPTY, map, Subject, switchMap } from 'rxjs';
import { AuthService } from '../../../auth/services/auth.service';
import { EntryCard } from '../../components/entry-card/entry-card';
import { EntryNavbar } from '../../components/entry-navbar/entry-navbar';
import { EntryEditorDialog } from '../../components/entry-editor-dialog/entry-editor-dialog';
import { EntryDeleteDialog } from '../../components/entry-delete-dialog/entry-delete-dialog';
import { EntryService } from '../../services/entry.service';
import { Entry } from '../../models/entry';
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

  protected readonly username = signal('');
  protected readonly entries = signal<Entry[]>([]);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly canEdit = computed(() => this.auth.user()?.username === this.username());

  constructor() {
    // Listen before the route emits the initial search criteria
    this.searchRequested
      .pipe(
        switchMap((criteria) => {
          this.errorMessage.set(null);

          return this.entryService.search(criteria).pipe(
            catchError((error: unknown) => {
              this.errorMessage.set('Unable to load this library.');
              console.error('Error during load entries list:', error);
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((page) => this.entries.set(page.items));

    this.route.paramMap
      .pipe(
        map((params) => params.get('username') ?? ''),
        distinctUntilChanged(),
        takeUntilDestroyed(),
      )
      .subscribe((username) => {
        this.username.set(username);
        this.entries.set([]);
        this.onCriteriaChange(new SearchCriteria());
      });
  }

  protected onCriteriaChange(criteria: SearchCriteria): void {
    this.currentCriteria = Object.assign(new EntrySearchCriteria(this.username()), criteria);
    this.searchRequested.next(this.currentCriteria);
  }

  protected onEntrySaved(entry: Entry): void {
    const isUpdate = this.entries().some((existing) => existing.id === entry.id);
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
}

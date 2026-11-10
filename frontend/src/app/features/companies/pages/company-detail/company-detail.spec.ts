import { ViewportScroller } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { GameSummary } from '../../../games/models/game';
import { Company } from '../../models/company';
import { CompanyDetail } from './company-detail';

function gamePage(items: GameSummary[], page = 0, totalElements = items.length) {
  return { items, page, size: 24, totalElements, totalPages: Math.ceil(totalElements / 24) };
}

describe('CompanyDetail', () => {
  let http: HttpTestingController;
  const company: Company = {
    id: 1,
    igdbId: 100,
    name: 'Example Studio',
    country: 'Italy',
    foundedDate: '05/1996',
    logoImageId: null,
    roles: ['developer', 'publisher'],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { paramMap: new BehaviorSubject(convertToParamMap({ id: '1' })) },
        },
        { provide: ViewportScroller, useValue: { scrollToPosition() {} } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('opens Developed first and replaces the in-flight list when switching to Published', async () => {
    const fixture = TestBed.createComponent(CompanyDetail);
    const page: HTMLElement = fixture.nativeElement;
    http.expectOne('/api/companies/1').flush(company);
    const developed = http.expectOne('/api/companies/1/games?role=developer&page=0');
    await fixture.whenStable();

    expect(page.querySelector('h1')?.textContent).toContain('Example Studio');
    expect(page.textContent).toContain('05/1996');
    expect(page.textContent).toContain('Italy');
    const tabs = Array.from(page.querySelectorAll('label.tab'));
    const radios = page.querySelectorAll<HTMLInputElement>('.tab input[type="radio"]');
    expect(tabs.map((tab) => tab.textContent?.trim())).toEqual(['Developed', 'Published']);
    expect(radios[0].checked).toBe(true);

    radios[1].click();
    expect(developed.cancelled).toBe(true);
    http
      .expectOne('/api/companies/1/games?role=publisher&page=0')
      .flush(gamePage([{ id: 42, igdbId: 420, name: 'Published Game', coverImageId: null }]));
    await fixture.whenStable();

    expect(radios[0].checked).toBe(false);
    expect(radios[1].checked).toBe(true);
    expect(page.querySelector('a[href="/game/42"]')?.textContent).toContain('Published Game');
  });

  it('opens Published and hides Developed when the company has no developer role', async () => {
    const fixture = TestBed.createComponent(CompanyDetail);
    const page: HTMLElement = fixture.nativeElement;
    http.expectOne('/api/companies/1').flush({ ...company, roles: ['publisher', 'porting'] });
    http.expectOne('/api/companies/1/games?role=publisher&page=0').flush(gamePage([]));
    await fixture.whenStable();

    const tabs = Array.from(page.querySelectorAll('label.tab'));
    expect(tabs.map((tab) => tab.textContent?.trim())).toEqual(['Published', 'Ported']);
    expect(page.querySelector<HTMLInputElement>('.tab input:checked')?.value).toBe('publisher');
    expect(page.textContent).toContain('No games available in the catalog.');
  });

  it('shows the selected tab when there is only one role', async () => {
    const fixture = TestBed.createComponent(CompanyDetail);
    const page: HTMLElement = fixture.nativeElement;
    http.expectOne('/api/companies/1').flush({ ...company, roles: ['publisher'] });
    http.expectOne('/api/companies/1/games?role=publisher&page=0').flush(gamePage([]));
    await fixture.whenStable();

    expect(page.querySelectorAll('label.tab')).toHaveLength(1);
    expect(page.querySelector('label.tab')?.textContent?.trim()).toBe('Published');
    expect(page.querySelector<HTMLInputElement>('.tab input:checked')?.value).toBe('publisher');
  });

  it('fetches the next page and resets to the first page when switching roles', async () => {
    const fixture = TestBed.createComponent(CompanyDetail);
    const page: HTMLElement = fixture.nativeElement;
    const first = { id: 42, igdbId: 420, name: 'First Game', coverImageId: null };
    const second = { id: 43, igdbId: 430, name: 'Second Game', coverImageId: null };
    http.expectOne('/api/companies/1').flush(company);
    http.expectOne('/api/companies/1/games?role=developer&page=0').flush(gamePage([first], 0, 25));
    await fixture.whenStable();

    page.querySelector<HTMLElement>('section:has(.tabs)')!.scrollIntoView = vi.fn();
    const buttons = page.querySelectorAll<HTMLButtonElement>('nav button');
    expect(buttons[0].disabled).toBe(true);
    buttons[1].click();
    http.expectOne('/api/companies/1/games?role=developer&page=1').flush(gamePage([second], 1, 25));
    await fixture.whenStable();

    expect(page.querySelector('a[href="/game/43"]')?.textContent).toContain('Second Game');
    expect(page.querySelector('a[href="/game/42"]')).toBeNull();
    expect(page.querySelector<HTMLButtonElement>('nav button:last-child')?.disabled).toBe(true);
    page.querySelector<HTMLInputElement>('input[value="publisher"]')!.click();
    http.expectOne('/api/companies/1/games?role=publisher&page=0').flush(gamePage([second]));
    await fixture.whenStable();
    expect(page.querySelector('nav')).toBeNull();
  });
});

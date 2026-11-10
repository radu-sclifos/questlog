import { ViewportScroller } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { Company } from '../../models/company';
import { CompanyDetail } from './company-detail';

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
    const developed = http.expectOne('/api/companies/1/games?role=developer');
    await fixture.whenStable();

    expect(page.querySelector('h1')?.textContent).toContain('Example Studio');
    expect(page.textContent).toContain('05/1996');
    expect(page.textContent).toContain('Italy');
    const buttons = Array.from(page.querySelectorAll('button'));
    expect(buttons.map((button) => button.textContent?.trim())).toEqual(['Developed', 'Published']);
    expect(buttons[0].classList).toContain('btn-primary');

    buttons[1].click();
    expect(developed.cancelled).toBe(true);
    http
      .expectOne('/api/companies/1/games?role=publisher')
      .flush([{ id: 42, igdbId: 420, name: 'Published Game', coverImageId: null }]);
    await fixture.whenStable();

    expect(buttons[1].classList).toContain('btn-primary');
    expect(page.querySelector('a[href="/game/42"]')?.textContent).toContain('Published Game');
  });

  it('opens Published and hides Developed when the company has no developer role', async () => {
    const fixture = TestBed.createComponent(CompanyDetail);
    const page: HTMLElement = fixture.nativeElement;
    http.expectOne('/api/companies/1').flush({ ...company, roles: ['publisher', 'porting'] });
    http.expectOne('/api/companies/1/games?role=publisher').flush([]);
    await fixture.whenStable();

    const buttons = Array.from(page.querySelectorAll('button'));
    expect(buttons.map((button) => button.textContent?.trim())).toEqual(['Published', 'Ported']);
    expect(buttons[0].classList).toContain('btn-primary');
    expect(page.textContent).toContain('No games available in the catalog.');
  });
});

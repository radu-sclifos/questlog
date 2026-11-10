import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Page } from '../../../shared/search/page';
import { GameSummary } from '../../games/models/game';
import { Company, CompanyRole } from '../models/company';

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private readonly http = inject(HttpClient);

  findById(id: number) {
    return this.http.get<Company>(`/api/companies/${id}`);
  }

  findGames(id: number, role: CompanyRole, page: number) {
    return this.http.get<Page<GameSummary>>(`/api/companies/${id}/games`, {
      params: { role, page },
    });
  }
}

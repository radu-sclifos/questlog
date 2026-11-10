import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { GameSummary } from '../../games/models/game';
import { Company, CompanyRole } from '../models/company';

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private readonly http = inject(HttpClient);

  findById(id: number) {
    return this.http.get<Company>(`/api/companies/${id}`);
  }

  findGames(id: number, role: CompanyRole) {
    return this.http.get<GameSummary[]>(`/api/companies/${id}/games`, { params: { role } });
  }
}

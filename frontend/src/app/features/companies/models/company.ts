export type CompanyRole = 'developer' | 'publisher' | 'porting' | 'supporting';

export interface Company {
  id: number;
  igdbId: number | null;
  name: string;
  country: string | null;
  foundedDate: string | null;
  logoImageId: string | null;
  roles: CompanyRole[];
}

export interface GameReference {
  id: number;
  igdbId: number;
  name: string;
}

export interface GameSummary extends GameReference {
  coverImageId: string | null;
}

export interface GameCompany {
  company: GameReference;
  developer: boolean;
  publisher: boolean;
  porting: boolean;
  supporting: boolean;
}

export type GameRelationType =
  | 'PARENT_GAME'
  | 'VERSION_OF'
  | 'DLC'
  | 'EXPANSION'
  | 'STANDALONE_EXPANSION'
  | 'REMAKE'
  | 'REMASTER'
  | 'PORT'
  | 'EXPANDED_GAME'
  | 'FORK';

export interface GameRelation {
  relationType: GameRelationType;
  game: GameSummary;
}

export interface Game extends GameSummary {
  firstReleaseDate: string | null;
  summary: string | null;
  storyline: string | null;
  gameType: GameReference | null;
  platforms: GameReference[];
  genres: GameReference[];
  engines: GameReference[];
  franchises: GameReference[];
  series: GameReference[];
  involvedCompanies: GameCompany[];
  relations: GameRelation[];
}

import { EntryGame } from './entry-game';

export enum EntryStatus {
  BEATEN = 'BEATEN',
  COMPLETED = 'COMPLETED',
  PLAYING = 'PLAYING',
  UNPLAYED = 'UNPLAYED',
  ON_HOLD = 'ON_HOLD',
  ENDLESS = 'ENDLESS',
  DROPPED = 'DROPPED',
}

export enum EntryOwnership {
  OWN = 'OWN',
  HOUSEHOLD = 'HOUSEHOLD',
  SUBSCRIPTION = 'SUBSCRIPTION',
  FORMERLY_OWNED = 'FORMERLY_OWNED',
  OTHER = 'OTHER'
}

export interface SaveEntryRequest {
  gameId: number;
  score: number | null;
  notes: string | null;
  status: EntryStatus | null;
  ownership: EntryOwnership | null;
  playtime: number | null;
}

export interface Entry {
  id: number;
  game: EntryGame;
  score: number | null;
  notes: string | null;
  status: EntryStatus;
  ownership: EntryOwnership | null;
  playtime: number | null;
}

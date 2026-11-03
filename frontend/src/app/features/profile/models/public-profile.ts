import { EntryStatus } from '../../entry/models/entry';
import { EntryGame } from '../../entry/models/entry-game';
import { Platform } from '../../games/models/platform';

export interface PublicProfile {
  username: string;
  displayName: string;
  bio: string | null;
  createdAt: string;
  avatarRevision: string | null;
  statistics: {
    totalGames: number;
    ratedGames: number;
    averageRating: number | null;
    finishedGames: number;
    loggedPlaytime: number | null;
    statuses: Record<EntryStatus, number>;
  };
  currentlyPlaying: { game: EntryGame; platform: Platform | null; playtime: number | null }[];
  favorites: EntryGame[];
}

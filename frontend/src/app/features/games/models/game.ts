export type GameStatus = 'Playing' | 'On-hold' | 'Dropped' | 'Completed' | 'Planned';

export interface Game {
  id: number;
  title: string;
  platform: string;
  coverImageId: string | null;
  rating: number | null;
  status: GameStatus;
  genre: string;
  ownership: string;
  hoursPlayed: number;
  achievementsUnlocked: number;
  achievementsTotal: number;
  notes: string;
}

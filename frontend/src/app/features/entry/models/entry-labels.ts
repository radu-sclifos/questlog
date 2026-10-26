import { EntryOwnership, EntryStatus } from './entry';

export const ENTRY_STATUS_LABELS: Readonly<Record<EntryStatus, string>> = {
  [EntryStatus.BEATEN]: 'Beaten',
  [EntryStatus.COMPLETED]: 'Completed',
  [EntryStatus.PLAYING]: 'Playing',
  [EntryStatus.UNPLAYED]: 'Unplayed',
  [EntryStatus.ON_HOLD]: 'On Hold',
  [EntryStatus.ENDLESS]: 'Endless',
  [EntryStatus.DROPPED]: 'Dropped',
};

export const ENTRY_OWNERSHIP_LABELS: Readonly<Record<EntryOwnership, string>> = {
  [EntryOwnership.OWN]: 'Own',
  [EntryOwnership.HOUSEHOLD]: 'Household',
  [EntryOwnership.SUBSCRIPTION]: 'Subscription',
  [EntryOwnership.FORMERLY_OWNED]: 'Formerly owned',
  [EntryOwnership.OTHER]: 'Other',
};

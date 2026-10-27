import { Entry } from './entry';

export interface EntryPage {
  items: Entry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

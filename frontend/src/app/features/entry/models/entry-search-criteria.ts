import { SearchCriteria } from '../../../shared/search/search-criteria';

export class EntrySearchCriteria extends SearchCriteria {
  username: string;

  constructor(username: string) {
    super();
    this.username = username;
  }
}

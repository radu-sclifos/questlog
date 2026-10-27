import { SearchCriteria } from '../../../shared/search/search-criteria';

export class EntrySearchCriteria extends SearchCriteria {
  /** Required, non-blank, at most 40 characters. */
  username: string;

  constructor(username: string) {
    super();
    this.username = username;
  }

}

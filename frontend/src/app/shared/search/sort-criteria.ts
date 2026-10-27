export enum SortDirection {
  ASC = 'ASC',
  DESC = 'DESC',
}

export interface SortCriteria {
  field: string;

  direction: SortDirection;
}

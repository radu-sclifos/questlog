export enum SortDirection {
  ASC = 'ASC',
  DESC = 'DESC',
}

export interface SortCriteria {
  field: string;

  direction: SortDirection;

  /** Enum values in priority order. ASC follows this order; DESC reverses it. */
  customOrder?: string[];
}

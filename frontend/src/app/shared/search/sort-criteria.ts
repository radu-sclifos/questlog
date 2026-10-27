export enum SortDirection {
  ASC = 'ASC',
  DESC = 'DESC',
}

export interface SortCriteria {
  field: string;

  direction: SortDirection;

  /** Enum values in priority order. ASC follows this order; DESC reverses it. */
  customOrder?: string[];

  /** Places null values last in both ASC and DESC; omitted uses database defaults. */
  nullsLast?: boolean;
}

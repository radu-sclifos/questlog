export enum FilterOperator {
  EQUAL = 'EQUAL',
  CONTAINS = 'CONTAINS',
  GREATER_THAN = 'GREATER_THAN',
  LESS_THAN = 'LESS_THAN',
  IN = 'IN',
  IS_NULL = 'IS_NULL',
}

export type FilterValue = string | number | boolean | null;

export interface FilterCriteria {
  field: string;

  operator: FilterOperator;

  /** Used by EQUAL, CONTAINS, GREATER_THAN and LESS_THAN; omitted for IS_NULL. */
  value?: FilterValue;

  /** Used by IN (1 to 100 values). */
  valueList?: FilterValue[] | null;
}

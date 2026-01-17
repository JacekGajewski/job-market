export type SalaryRange = 'UNDER_25K' | 'RANGE_25_30K' | 'OVER_30K';

export const SALARY_RANGES: SalaryRange[] = ['UNDER_25K', 'RANGE_25_30K', 'OVER_30K'];

export const SALARY_RANGE_LABELS: Record<SalaryRange, string> = {
  UNDER_25K: '< 25k',
  RANGE_25_30K: '25-30k',
  OVER_30K: '> 30k'
};

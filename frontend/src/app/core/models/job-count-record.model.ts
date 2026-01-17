export type MetricType = 'TOTAL' | 'WITH_SALARY' | 'REMOTE' | 'REMOTE_WITH_SALARY';

export interface JobCountRecord {
  id: number;
  category: string;
  count: number;
  fetchedAt: string;
  location: string;
  metricType: MetricType;
}

export interface CategoryStats {
  category: string;
  records: JobCountRecord[];
  latestCount: number;
  previousCount: number;
  changePercent: number;
}

export interface LatestCount {
  category: string;
  metricType: MetricType;
  count: number;
  fetchedAt: string;
  changeFromPrevious: number | null;
  percentageChange: number | null;
}

export const METRIC_TYPE_LABELS: Record<MetricType, string> = {
  TOTAL: 'Total',
  WITH_SALARY: 'With Salary',
  REMOTE: 'Remote',
  REMOTE_WITH_SALARY: 'Remote + Salary'
};

export interface DashboardSummary {
  totalJobs: number;
  averageGrowth: number;
  topPerformer: {
    category: string;
    changePercent: number;
  } | null;
  categoryCount: number;
}

export interface DateRange {
  startDate?: Date;
  endDate?: Date;
}

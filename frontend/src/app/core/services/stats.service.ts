import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { JobCountRecord, CategoryStats, LatestCount, DateRange, MetricType } from '../models/job-count-record.model';
import { ExperienceLevel } from '../models/experience-level.model';
import { SalaryRange } from '../models/salary-range.model';

export interface StatsFilterOptions {
  dateRange?: DateRange;
  metricType?: MetricType;
  city?: string;
  experienceLevel?: ExperienceLevel;
  salaryRange?: SalaryRange;
}

@Injectable({
  providedIn: 'root'
})
export class StatsService {
  private api = inject(ApiService);

  async getCategoryStats(categorySlug: string, options?: StatsFilterOptions): Promise<CategoryStats> {
    let endpoint = `/api/stats/${categorySlug}`;

    const params: string[] = [];
    params.push(`metricType=${options?.metricType ?? 'TOTAL'}`);

    if (options?.city) {
      params.push(`city=${options.city}`);
    }
    if (options?.experienceLevel) {
      params.push(`experienceLevel=${options.experienceLevel}`);
    }
    if (options?.salaryRange) {
      params.push(`salaryRange=${options.salaryRange}`);
    }
    if (options?.dateRange?.startDate) {
      params.push(`startDate=${this.formatDate(options.dateRange.startDate)}`);
    }
    if (options?.dateRange?.endDate) {
      params.push(`endDate=${this.formatDate(options.dateRange.endDate)}`);
    }
    endpoint += `?${params.join('&')}`;

    const records = await this.api.get<JobCountRecord[]>(endpoint);

    const sortedRecords = [...records].sort(
      (a, b) => new Date(b.fetchedAt).getTime() - new Date(a.fetchedAt).getTime()
    );

    const latestCount = sortedRecords[0]?.count ?? 0;
    const previousCount = sortedRecords[1]?.count ?? latestCount;
    const changePercent = previousCount > 0
      ? ((latestCount - previousCount) / previousCount) * 100
      : 0;

    return {
      category: categorySlug,
      records: sortedRecords,
      latestCount,
      previousCount,
      changePercent
    };
  }

  async getLatestCount(categorySlug: string, options?: StatsFilterOptions): Promise<LatestCount> {
    const params: string[] = [];
    params.push(`metricType=${options?.metricType ?? 'TOTAL'}`);

    if (options?.city) {
      params.push(`city=${options.city}`);
    }
    if (options?.experienceLevel) {
      params.push(`experienceLevel=${options.experienceLevel}`);
    }
    if (options?.salaryRange) {
      params.push(`salaryRange=${options.salaryRange}`);
    }

    return this.api.get<LatestCount>(`/api/stats/${categorySlug}/latest?${params.join('&')}`);
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }
}

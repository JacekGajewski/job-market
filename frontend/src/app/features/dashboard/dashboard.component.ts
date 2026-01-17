import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSelectModule } from '@angular/material/select';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { NgChartsModule } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import { CategoryService } from '../../core/services/category.service';
import { CityService } from '../../core/services/city.service';
import { StatsService } from '../../core/services/stats.service';
import { Category, CategoryStats, DashboardSummary, DateRange, MetricType, METRIC_TYPE_LABELS } from '../../core/models';
import { City } from '../../core/models/city.model';
import { ExperienceLevel, EXPERIENCE_LEVELS, EXPERIENCE_LEVEL_LABELS } from '../../core/models/experience-level.model';
import { SalaryRange, SALARY_RANGES, SALARY_RANGE_LABELS } from '../../core/models/salary-range.model';
import { getCategoryColor } from './category-colors';

const METRIC_TYPES: MetricType[] = ['TOTAL', 'WITH_SALARY', 'REMOTE', 'REMOTE_WITH_SALARY'];

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    MatButtonToggleModule,
    MatSelectModule,
    MatMenuModule,
    MatDividerModule,
    NgChartsModule
  ],
  template: `
    <div class="dashboard-container">
      <!-- Header with Category Selector -->
      <header class="dashboard-header">
        <div class="header-left">
          <h1>Job Market Trends</h1>
          <p class="subtitle">Track technology job market dynamics in real-time</p>
        </div>
        <div class="header-right">
          <mat-button-toggle-group
            [value]="selectedCategory()"
            (change)="onCategoryChange($event.value)"
            class="category-selector">
            @for (category of activeCategories(); track category.id) {
              <mat-button-toggle
                [value]="category.slug"
                [style.--toggle-color]="getCategoryColor(category.slug).primary">
                {{ category.name }}
              </mat-button-toggle>
            }
          </mat-button-toggle-group>
          <button mat-icon-button
                  class="refresh-btn"
                  (click)="loadData()"
                  [disabled]="loading()"
                  matTooltip="Refresh data">
            <mat-icon>refresh</mat-icon>
          </button>
        </div>
      </header>

      <!-- Metric Type Selector -->
      <div class="metric-selector-row">
        <mat-button-toggle-group
          [value]="selectedMetricType()"
          (change)="onMetricTypeChange($event.value)"
          class="metric-selector">
          @for (metric of metricTypes; track metric) {
            <mat-button-toggle [value]="metric">
              {{ getMetricLabel(metric) }}
            </mat-button-toggle>
          }
        </mat-button-toggle-group>
      </div>

      <!-- Additional Filters Row -->
      <div class="additional-filters-row">
        <!-- City Selector -->
        <mat-button-toggle-group
          [value]="selectedCity"
          (change)="onCityChange($event.value)"
          class="filter-toggle">
          <mat-button-toggle [value]="null">All</mat-button-toggle>
          <mat-button-toggle value="wroclaw">Wrocław</mat-button-toggle>
          <mat-button-toggle value="slask">Śląsk</mat-button-toggle>
        </mat-button-toggle-group>

        <!-- Experience Level Selector -->
        <mat-button-toggle-group
          [value]="selectedExperience"
          (change)="onExperienceChange($event.value)"
          class="filter-toggle">
          <mat-button-toggle [value]="null">All Levels</mat-button-toggle>
          @for (level of experienceLevels; track level) {
            <mat-button-toggle [value]="level">{{ getExperienceLabel(level) }}</mat-button-toggle>
          }
        </mat-button-toggle-group>

        <!-- Salary Range Selector -->
        <mat-button-toggle-group
          [value]="selectedSalaryRange"
          (change)="onSalaryRangeChange($event.value)"
          class="filter-toggle">
          <mat-button-toggle [value]="null">Any Salary</mat-button-toggle>
          @for (range of salaryRanges; track range) {
            <mat-button-toggle [value]="range">{{ getSalaryRangeLabel(range) }}</mat-button-toggle>
          }
        </mat-button-toggle-group>
      </div>

      <!-- Loading State -->
      @if (loading()) {
        <div class="loading-container">
          <mat-spinner diameter="50"></mat-spinner>
          <p>Loading dashboard data...</p>
        </div>
      }

      <!-- Error State -->
      @if (error()) {
        <div class="error-container">
          <mat-icon>error_outline</mat-icon>
          <p>{{ error() }}</p>
          <button mat-raised-button (click)="loadData()">Retry</button>
        </div>
      }

      <!-- Main Content -->
      @if (!loading() && !error() && currentStats()) {
        <!-- Hero Stats Section -->
        <div class="hero-stats" [style.--accent-color]="currentCategoryColor().primary"
                                [style.--accent-glow]="currentCategoryColor().glow">
          <div class="hero-main">
            <div class="hero-count">{{ currentStats()!.latestCount | number }}</div>
            <div class="hero-label">{{ selectedCategoryName() }} {{ getMetricLabel(selectedMetricType()) }} Positions</div>
          </div>
          <div class="hero-trend" [class]="getTrendClass(currentStats()!)">
            <mat-icon>{{ getTrendIcon(currentStats()!) }}</mat-icon>
            <div class="trend-details">
              <span class="trend-value">{{ formatChange(currentStats()!) }}</span>
              <span class="trend-label">vs previous</span>
            </div>
          </div>
        </div>

        <!-- Stats Summary Row -->
        <div class="stats-row">
          <div class="stat-item">
            <mat-icon>calendar_today</mat-icon>
            <div class="stat-content">
              <span class="stat-value">{{ currentStats()!.records.length }}</span>
              <span class="stat-label">Data Points</span>
            </div>
          </div>
          <div class="stat-item">
            <mat-icon>show_chart</mat-icon>
            <div class="stat-content">
              <span class="stat-value">{{ getMinCount() | number }}</span>
              <span class="stat-label">Min Count</span>
            </div>
          </div>
          <div class="stat-item">
            <mat-icon>trending_up</mat-icon>
            <div class="stat-content">
              <span class="stat-value">{{ getMaxCount() | number }}</span>
              <span class="stat-label">Max Count</span>
            </div>
          </div>
          <div class="stat-item">
            <mat-icon>functions</mat-icon>
            <div class="stat-content">
              <span class="stat-value">{{ getAvgCount() | number:'1.0-0' }}</span>
              <span class="stat-label">Average</span>
            </div>
          </div>
        </div>

        <!-- Date Filter (hidden for now)
        <div class="filters-row">
          <div class="date-range-picker">
            <mat-form-field appearance="outline" class="date-field">
              <mat-label>Start Date</mat-label>
              <input matInput [matDatepicker]="startPicker" [(ngModel)]="startDate" (dateChange)="onDateChange()">
              <mat-datepicker-toggle matIconSuffix [for]="startPicker"></mat-datepicker-toggle>
              <mat-datepicker #startPicker></mat-datepicker>
            </mat-form-field>

            <mat-form-field appearance="outline" class="date-field">
              <mat-label>End Date</mat-label>
              <input matInput [matDatepicker]="endPicker" [(ngModel)]="endDate" (dateChange)="onDateChange()">
              <mat-datepicker-toggle matIconSuffix [for]="endPicker"></mat-datepicker-toggle>
              <mat-datepicker #endPicker></mat-datepicker>
            </mat-form-field>

            @if (startDate || endDate) {
              <button mat-icon-button class="clear-dates" (click)="clearDateRange()" matTooltip="Clear date filter">
                <mat-icon>clear</mat-icon>
              </button>
            }
          </div>
        </div>
        -->

        <!-- Chart Card -->
        <mat-card class="chart-card" [style.--card-accent]="currentCategoryColor().primary">
          <div class="card-accent-bar"></div>
          <mat-card-header>
            <mat-card-title>{{ selectedCategoryName() }} Job Count Trends</mat-card-title>
            <mat-card-subtitle>Historical data over time</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            @if (lineChartData.datasets.length > 0) {
              <div class="chart-container">
                <canvas baseChart
                  [data]="lineChartData"
                  [options]="lineChartOptions"
                  [type]="lineChartType">
                </canvas>
              </div>
            } @else {
              <div class="no-data">
                <mat-icon>show_chart</mat-icon>
                <p>No trend data available yet</p>
              </div>
            }
          </mat-card-content>
        </mat-card>
      }

      @if (!loading() && !error() && !currentStats()) {
        <div class="no-data-container">
          <mat-icon>info_outline</mat-icon>
          <p>No data available for this category yet</p>
        </div>
      }
    </div>
  `,
  styles: [`
    .dashboard-container {
      padding: 32px;
      max-width: 1200px;
      margin: 0 auto;
      min-height: 100%;
    }

    .dashboard-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 16px;
      gap: 24px;

      .header-left {
        h1 {
          margin: 0;
          font-size: 2.5rem;
          font-weight: 700;
          background: linear-gradient(135deg, #ffffff 0%, rgba(255, 255, 255, 0.7) 100%);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
        }

        .subtitle {
          margin: 8px 0 0;
          color: var(--text-secondary);
          font-size: 1rem;
        }
      }

      .header-right {
        display: flex;
        align-items: center;
        gap: 16px;
      }

      .category-selector {
        background: var(--bg-card);
        border: 1px solid var(--glass-border);
        border-radius: var(--border-radius-md);

        ::ng-deep {
          .mat-button-toggle {
            background: transparent;
            color: var(--text-secondary);
            border: none !important;

            .mat-button-toggle-label-content {
              padding: 0 20px;
              line-height: 40px;
              font-weight: 500;
            }

            &.mat-button-toggle-checked {
              background: color-mix(in srgb, var(--toggle-color, #00d4ff) 20%, transparent);
              color: var(--toggle-color, #00d4ff);
            }

            &:hover:not(.mat-button-toggle-checked) {
              background: rgba(255, 255, 255, 0.05);
            }
          }

          .mat-button-toggle-appearance-standard {
            background: transparent;
          }
        }
      }

      .refresh-btn {
        background: var(--bg-card);
        border: 1px solid var(--glass-border);
        color: var(--text-secondary);
        transition: all var(--transition-normal);

        &:hover:not(:disabled) {
          background: var(--bg-card-hover);
          color: var(--text-primary);
        }
      }
    }

    .metric-selector-row {
      display: flex;
      justify-content: center;
      margin-bottom: 16px;

      .metric-selector {
        background: var(--bg-card);
        border: 1px solid var(--glass-border);
        border-radius: var(--border-radius-md);

        ::ng-deep {
          .mat-button-toggle {
            background: transparent;
            color: var(--text-secondary);
            border: none !important;

            .mat-button-toggle-label-content {
              padding: 0 16px;
              line-height: 36px;
              font-weight: 500;
              font-size: 0.875rem;
            }

            &.mat-button-toggle-checked {
              background: rgba(255, 255, 255, 0.1);
              color: var(--text-primary);
            }

            &:hover:not(.mat-button-toggle-checked) {
              background: rgba(255, 255, 255, 0.05);
            }
          }

          .mat-button-toggle-appearance-standard {
            background: transparent;
          }
        }
      }
    }

    .additional-filters-row {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 16px;
      margin-bottom: 24px;
      padding: 16px 24px;
      background: var(--bg-card);
      backdrop-filter: blur(var(--glass-blur));
      border: 1px solid var(--glass-border);
      border-radius: var(--border-radius-md);
      flex-wrap: wrap;

      .city-dropdown {
        .city-trigger {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 6px 12px 6px 10px;
          background: rgba(255, 255, 255, 0.03);
          border: 1px solid var(--glass-border);
          border-radius: var(--border-radius-md);
          color: var(--text-primary);
          font-weight: 500;
          font-size: 0.875rem;
          transition: all 0.2s ease;
          min-width: 160px;
          justify-content: flex-start;

          mat-icon {
            font-size: 18px;
            width: 18px;
            height: 18px;
            color: var(--text-secondary);
          }

          .city-label {
            flex: 1;
            text-align: left;
          }

          .dropdown-arrow {
            margin-left: auto;
            transition: transform 0.2s ease;
          }

          &:hover {
            background: rgba(255, 255, 255, 0.08);
            border-color: rgba(255, 255, 255, 0.2);
          }

          &[aria-expanded="true"] {
            background: rgba(255, 255, 255, 0.1);
            border-color: var(--accent-cyan);

            .dropdown-arrow {
              transform: rotate(180deg);
            }
          }
        }
      }

      .filter-toggle {
        background: rgba(255, 255, 255, 0.03);
        border: 1px solid var(--glass-border);
        border-radius: var(--border-radius-md);

        ::ng-deep {
          .mat-button-toggle {
            background: transparent;
            color: var(--text-secondary);
            border: none !important;

            .mat-button-toggle-label-content {
              padding: 0 12px;
              line-height: 32px;
              font-weight: 500;
              font-size: 0.813rem;
            }

            &.mat-button-toggle-checked {
              background: rgba(255, 255, 255, 0.1);
              color: var(--text-primary);
            }

            &:hover:not(.mat-button-toggle-checked) {
              background: rgba(255, 255, 255, 0.05);
            }
          }

          .mat-button-toggle-appearance-standard {
            background: transparent;
          }
        }
      }
    }

    .hero-stats {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 40px 48px;
      margin-bottom: 24px;
      background: var(--bg-card);
      backdrop-filter: blur(var(--glass-blur));
      border: 1px solid var(--glass-border);
      border-radius: var(--border-radius-lg);
      border-left: 4px solid var(--accent-color);
      box-shadow: 0 0 30px var(--accent-glow);

      .hero-main {
        .hero-count {
          font-size: 4rem;
          font-weight: 700;
          color: var(--text-primary);
          line-height: 1;
          margin-bottom: 8px;
        }

        .hero-label {
          font-size: 1.125rem;
          color: var(--text-secondary);
        }
      }

      .hero-trend {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 16px 24px;
        background: rgba(255, 255, 255, 0.03);
        border-radius: var(--border-radius-md);

        mat-icon {
          font-size: 32px;
          width: 32px;
          height: 32px;
        }

        .trend-details {
          display: flex;
          flex-direction: column;

          .trend-value {
            font-size: 1.5rem;
            font-weight: 600;
          }

          .trend-label {
            font-size: 0.813rem;
            color: var(--text-muted);
          }
        }

        &.trend-up {
          mat-icon, .trend-value { color: var(--color-success); }
        }
        &.trend-down {
          mat-icon, .trend-value { color: var(--color-error); }
        }
        &.trend-flat {
          mat-icon, .trend-value { color: var(--text-muted); }
        }
      }
    }

    .stats-row {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;
      margin-bottom: 24px;

      .stat-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 16px 20px;
        background: var(--bg-card);
        backdrop-filter: blur(var(--glass-blur));
        border: 1px solid var(--glass-border);
        border-radius: var(--border-radius-md);

        mat-icon {
          font-size: 24px;
          width: 24px;
          height: 24px;
          color: var(--text-muted);
        }

        .stat-content {
          display: flex;
          flex-direction: column;

          .stat-value {
            font-size: 1.25rem;
            font-weight: 600;
            color: var(--text-primary);
          }

          .stat-label {
            font-size: 0.75rem;
            color: var(--text-muted);
          }
        }
      }
    }

    .filters-row {
      display: flex;
      justify-content: center;
      margin-bottom: 24px;
      padding: 16px 24px;
      background: var(--bg-card);
      backdrop-filter: blur(var(--glass-blur));
      border: 1px solid var(--glass-border);
      border-radius: var(--border-radius-md);
    }

    .date-range-picker {
      display: flex;
      align-items: center;
      gap: 12px;

      .date-field {
        width: 160px;

        ::ng-deep {
          .mat-mdc-form-field-subscript-wrapper { display: none; }
        }
      }

      .clear-dates {
        color: var(--text-secondary);

        &:hover { color: var(--color-error); }
      }
    }

    .chart-card {
      position: relative;
      overflow: hidden;
      min-height: 450px;

      .card-accent-bar {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        height: 3px;
        background: var(--card-accent);
      }

      mat-card-title {
        color: var(--text-primary);
        font-weight: 600;
      }

      mat-card-subtitle {
        color: var(--text-secondary);
      }

      .chart-container {
        position: relative;
        height: 380px;
        margin-top: 16px;
      }
    }

    .loading-container, .error-container, .no-data-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 400px;
      gap: 20px;

      p {
        color: var(--text-secondary);
        font-size: 1rem;
      }

      mat-icon {
        font-size: 56px;
        width: 56px;
        height: 56px;
        color: var(--text-muted);
      }
    }

    .error-container mat-icon {
      color: var(--color-error);
    }

    .no-data {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 350px;

      mat-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        color: var(--text-muted);
        margin-bottom: 16px;
      }

      p {
        color: var(--text-muted);
        font-size: 1rem;
      }
    }

    @media (max-width: 1024px) {
      .dashboard-container { padding: 24px; }

      .stats-row {
        grid-template-columns: repeat(2, 1fr);
      }

      .hero-stats {
        flex-direction: column;
        text-align: center;
        gap: 24px;
        padding: 32px;

        .hero-main .hero-count {
          font-size: 3rem;
        }
      }
    }

    @media (max-width: 768px) {
      .dashboard-container { padding: 16px; }

      .dashboard-header {
        flex-direction: column;
        gap: 16px;

        .header-left h1 { font-size: 1.75rem; }
        .subtitle { font-size: 0.875rem; }

        .header-right {
          width: 100%;
          justify-content: space-between;
        }

        .category-selector {
          flex: 1;
        }
      }

      .stats-row {
        grid-template-columns: repeat(2, 1fr);
      }

      .hero-stats {
        padding: 24px;

        .hero-main .hero-count {
          font-size: 2.5rem;
        }

        .hero-trend {
          padding: 12px 16px;

          mat-icon {
            font-size: 24px;
            width: 24px;
            height: 24px;
          }

          .trend-value {
            font-size: 1.25rem;
          }
        }
      }

      .date-range-picker {
        flex-direction: column;
        width: 100%;

        .date-field { width: 100%; }
      }
    }
  `]
})
export class DashboardComponent implements OnInit {
  private categoryService = inject(CategoryService);
  private cityService = inject(CityService);
  private statsService = inject(StatsService);

  categories = signal<Category[]>([]);
  cities = signal<City[]>([]);
  categoryStats = signal<Map<string, CategoryStats>>(new Map());
  loading = signal(true);
  error = signal<string | null>(null);
  selectedCategory = signal<string>('java');
  selectedMetricType = signal<MetricType>('TOTAL');
  metricTypes = METRIC_TYPES;

  // New filter selections
  selectedCity: string | null = null;
  selectedExperience: ExperienceLevel | null = null;
  selectedSalaryRange: SalaryRange | null = null;

  // Filter options
  experienceLevels = EXPERIENCE_LEVELS;
  salaryRanges = SALARY_RANGES;

  // Computed signals for current selection
  activeCategories = computed(() => this.categories().filter(c => c.active));
  activeCities = computed(() => this.cities().filter(c => c.active));

  currentStats = computed(() => {
    const slug = this.selectedCategory();
    return this.categoryStats().get(slug) || null;
  });

  currentCategoryColor = computed(() => getCategoryColor(this.selectedCategory()));

  selectedCategoryName = computed(() => {
    const cat = this.categories().find(c => c.slug === this.selectedCategory());
    return cat?.name || this.selectedCategory();
  });

  startDate: Date | null = null;
  endDate: Date | null = null;

  lineChartType = 'line' as const;
  lineChartData: ChartData<'line'> = { labels: [], datasets: [] };
  lineChartOptions: ChartOptions<'line'> = this.createChartOptions();

  getCategoryColor = getCategoryColor;
  getMetricLabel = (metric: MetricType) => METRIC_TYPE_LABELS[metric];
  getExperienceLabel = (level: ExperienceLevel) => EXPERIENCE_LEVEL_LABELS[level];
  getSalaryRangeLabel = (range: SalaryRange) => SALARY_RANGE_LABELS[range];

  ngOnInit(): void {
    this.loadData();
  }

  async loadData(): Promise<void> {
    try {
      this.loading.set(true);
      this.error.set(null);

      const [cats, allCities] = await Promise.all([
        this.categoryService.getCategories(),
        this.cityService.getCities()
      ]);
      this.categories.set(cats);
      this.cities.set(allCities);

      const activeCats = cats.filter(c => c.active);

      // Set default selected category to first active one if current is not active
      if (activeCats.length > 0 && !activeCats.find(c => c.slug === this.selectedCategory())) {
        this.selectedCategory.set(activeCats[0].slug);
      }

      const statsMap = new Map<string, CategoryStats>();
      for (const cat of activeCats) {
        try {
          const stats = await this.statsService.getCategoryStats(cat.slug, {
            dateRange: {
              startDate: this.startDate || undefined,
              endDate: this.endDate || undefined
            },
            metricType: this.selectedMetricType(),
            city: this.selectedCity || undefined,
            experienceLevel: this.selectedExperience || undefined,
            salaryRange: this.selectedSalaryRange || undefined
          });
          statsMap.set(cat.slug, stats);
        } catch {
          // Category has no data yet
        }
      }
      this.categoryStats.set(statsMap);

      this.updateChart();
    } catch (err) {
      this.error.set('Failed to load dashboard data');
      console.error(err);
    } finally {
      this.loading.set(false);
    }
  }

  onCategoryChange(slug: string): void {
    this.selectedCategory.set(slug);
    this.updateChart();
  }

  onMetricTypeChange(metricType: MetricType): void {
    this.selectedMetricType.set(metricType);
    this.loadData();
  }

  onFilterChange(): void {
    this.loadData();
  }

  onCityChange(slug: string | null): void {
    this.selectedCity = slug;
    this.loadData();
  }

  getSelectedCityName(): string {
    if (!this.selectedCity) return 'All Locations';
    const city = this.cities().find(c => c.slug === this.selectedCity);
    return city?.name || 'All Locations';
  }

  onExperienceChange(level: ExperienceLevel | null): void {
    this.selectedExperience = level;
    this.loadData();
  }

  onSalaryRangeChange(range: SalaryRange | null): void {
    this.selectedSalaryRange = range;
    this.loadData();
  }

  onDateChange(): void {
    this.loadData();
  }

  clearDateRange(): void {
    this.startDate = null;
    this.endDate = null;
    this.loadData();
  }

  getMinCount(): number {
    const stats = this.currentStats();
    if (!stats || stats.records.length === 0) return 0;
    return Math.min(...stats.records.map(r => r.count));
  }

  getMaxCount(): number {
    const stats = this.currentStats();
    if (!stats || stats.records.length === 0) return 0;
    return Math.max(...stats.records.map(r => r.count));
  }

  getAvgCount(): number {
    const stats = this.currentStats();
    if (!stats || stats.records.length === 0) return 0;
    const sum = stats.records.reduce((acc, r) => acc + r.count, 0);
    return sum / stats.records.length;
  }

  private createChartOptions(): ChartOptions<'line'> {
    return {
      responsive: true,
      maintainAspectRatio: false,
      animation: {
        duration: 750,
        easing: 'easeInOutQuart'
      },
      interaction: {
        mode: 'index',
        intersect: false
      },
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          mode: 'index',
          intersect: false,
          backgroundColor: 'rgba(15, 15, 35, 0.95)',
          titleColor: '#ffffff',
          bodyColor: 'rgba(255, 255, 255, 0.8)',
          borderColor: 'rgba(255, 255, 255, 0.1)',
          borderWidth: 1,
          cornerRadius: 8,
          padding: 12,
          titleFont: { family: 'Inter', weight: 'bold', size: 14 },
          bodyFont: { family: 'Inter', size: 13 },
          callbacks: {
            label: (context) => {
              const value = context.parsed.y;
              return `${value?.toLocaleString() ?? 0} jobs`;
            }
          }
        }
      },
      scales: {
        x: {
          display: true,
          grid: {
            color: 'rgba(255, 255, 255, 0.05)'
          },
          ticks: {
            color: 'rgba(255, 255, 255, 0.6)',
            font: { family: 'Inter', size: 11 }
          }
        },
        y: {
          display: true,
          beginAtZero: false,
          grid: {
            color: 'rgba(255, 255, 255, 0.05)'
          },
          ticks: {
            color: 'rgba(255, 255, 255, 0.6)',
            font: { family: 'Inter', size: 11 },
            callback: (value) => value.toLocaleString()
          }
        }
      }
    };
  }

  private updateChart(): void {
    const stats = this.currentStats();
    if (!stats || stats.records.length === 0) {
      this.lineChartData = { labels: [], datasets: [] };
      return;
    }

    const color = this.currentCategoryColor();
    const sortedRecords = [...stats.records].sort((a, b) =>
      a.fetchedAt.localeCompare(b.fetchedAt)
    );

    this.lineChartData = {
      labels: sortedRecords.map(r =>
        new Date(r.fetchedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
      ),
      datasets: [{
        label: this.selectedCategoryName(),
        data: sortedRecords.map(r => r.count),
        borderColor: color.primary,
        backgroundColor: color.gradient,
        fill: true,
        tension: 0.4,
        borderWidth: 2,
        pointRadius: 4,
        pointHoverRadius: 6,
        pointBackgroundColor: color.primary,
        pointBorderColor: 'rgba(15, 15, 35, 1)',
        pointBorderWidth: 2
      }]
    };
  }

  getTrendIcon(stats: CategoryStats): string {
    if (stats.changePercent > 0) return 'trending_up';
    if (stats.changePercent < 0) return 'trending_down';
    return 'trending_flat';
  }

  getTrendClass(stats: CategoryStats): string {
    if (stats.changePercent > 0) return 'trend-up';
    if (stats.changePercent < 0) return 'trend-down';
    return 'trend-flat';
  }

  formatChange(stats: CategoryStats): string {
    const sign = stats.changePercent > 0 ? '+' : '';
    return `${sign}${stats.changePercent.toFixed(1)}%`;
  }
}

# Angular Frontend Implementation Plan

## Overview
Angular 17+ with Angular Material and ng2-charts for job market trend visualization.

## 1. Project Setup

```bash
cd /Users/gajewskij/Projects/job-market
ng new frontend --routing --style=scss --standalone --skip-git
cd frontend
ng add @angular/material
npm install ng2-charts chart.js date-fns
```

## 2. Project Structure

```
frontend/src/app/
├── core/
│   ├── services/
│   │   ├── api.service.ts
│   │   ├── category.service.ts
│   │   └── stats.service.ts
│   ├── interceptors/
│   │   └── http-error.interceptor.ts
│   └── models/
│       ├── category.model.ts
│       └── job-count-record.model.ts
├── shared/
│   └── components/
│       ├── loading-spinner/
│       └── confirm-dialog/
├── features/
│   ├── dashboard/
│   │   ├── dashboard.component.ts
│   │   └── components/
│   │       ├── trend-chart/
│   │       └── stats-card/
│   └── categories/
│       ├── categories.component.ts
│       └── components/
│           └── add-category-dialog/
├── layout/
│   ├── header/
│   └── sidenav/
├── app.component.ts
├── app.config.ts
└── app.routes.ts
```

## 3. Data Models

### category.model.ts
```typescript
export interface Category {
  id: number;
  name: string;
  slug: string;
  active: boolean;
}

export interface CreateCategoryRequest {
  name: string;
  slug: string;
}
```

### job-count-record.model.ts
```typescript
export interface JobCountRecord {
  id: number;
  category: string;
  count: number;
  fetchedAt: string;
  location: string;
}

export interface CategoryStats {
  category: string;
  records: JobCountRecord[];
  latestCount: number;
  changePercent: number;
}
```

## 4. Services

### api.service.ts
```typescript
@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private baseUrl = environment.apiUrl;

  async get<T>(endpoint: string): Promise<T> {
    return firstValueFrom(this.http.get<T>(`${this.baseUrl}${endpoint}`));
  }

  async post<T>(endpoint: string, body: unknown): Promise<T> {
    return firstValueFrom(this.http.post<T>(`${this.baseUrl}${endpoint}`, body));
  }

  async delete<T>(endpoint: string): Promise<T> {
    return firstValueFrom(this.http.delete<T>(`${this.baseUrl}${endpoint}`));
  }
}
```

### category.service.ts
```typescript
@Injectable({ providedIn: 'root' })
export class CategoryService {
  private api = inject(ApiService);

  getCategories(): Promise<Category[]> {
    return this.api.get('/api/categories');
  }

  createCategory(request: CreateCategoryRequest): Promise<Category> {
    return this.api.post('/api/categories', request);
  }

  deleteCategory(id: number): Promise<void> {
    return this.api.delete(`/api/categories/${id}`);
  }
}
```

## 5. Dashboard Component

```typescript
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatCardModule, TrendChartComponent, StatsCardComponent]
})
export class DashboardComponent implements OnInit {
  categories: Category[] = [];
  categoryStats = new Map<string, CategoryStats>();
  loading = true;

  async ngOnInit() {
    this.categories = await this.categoryService.getCategories();
    // Load stats for each category
  }
}
```

## 6. Trend Chart Component

```typescript
@Component({
  selector: 'app-trend-chart',
  standalone: true,
  imports: [BaseChartDirective]
})
export class TrendChartComponent implements OnChanges {
  @Input() categories: Category[] = [];
  @Input() statsMap = new Map<string, CategoryStats>();

  lineChartType: ChartType = 'line';
  lineChartData: ChartData<'line'> = { labels: [], datasets: [] };

  lineChartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: true, position: 'top' },
      tooltip: { mode: 'index', intersect: false }
    },
    scales: {
      x: { display: true, title: { display: true, text: 'Date' } },
      y: { display: true, title: { display: true, text: 'Job Count' } }
    }
  };

  ngOnChanges() {
    this.updateChartData();
  }
}
```

## 7. Stats Card Component

```typescript
@Component({
  selector: 'app-stats-card',
  standalone: true,
  imports: [MatCardModule, MatIconModule]
})
export class StatsCardComponent {
  @Input() category!: Category;
  @Input() stats!: CategoryStats;

  get trendIcon(): string {
    if (this.stats.changePercent > 0) return 'trending_up';
    if (this.stats.changePercent < 0) return 'trending_down';
    return 'trending_flat';
  }
}
```

## 8. Routing

### app.routes.ts
```typescript
export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/dashboard/dashboard.component')
      .then(m => m.DashboardComponent)
  },
  {
    path: 'categories',
    loadComponent: () => import('./features/categories/categories.component')
      .then(m => m.CategoriesComponent)
  },
  { path: '**', redirectTo: '' }
];
```

## 9. Environment Configuration

### environment.ts (dev)
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'
};
```

### environment.prod.ts
```typescript
export const environment = {
  production: true,
  apiUrl: '/api'  // Nginx proxies to backend
};
```

## 10. Proxy Configuration (dev)

### proxy.conf.json
```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

## 11. Angular Material Theme

### styles/_theme.scss
```scss
@use '@angular/material' as mat;

@include mat.core();

$primary: mat.define-palette(mat.$indigo-palette);
$accent: mat.define-palette(mat.$pink-palette, A200);
$theme: mat.define-light-theme((
  color: (primary: $primary, accent: $accent)
));

@include mat.all-component-themes($theme);
```

## 12. Implementation Sequence

1. **Day 1**: Project setup, dependencies, environment config
2. **Day 1-2**: Core services, models, interceptors
3. **Day 2**: Layout components (header, sidenav)
4. **Day 2-3**: Dashboard with charts
5. **Day 3**: Categories management
6. **Day 4**: Polish, responsive styles, testing

## 13. Commands

```bash
ng serve                              # Dev server
ng serve --proxy-config proxy.conf.json  # With API proxy
ng build --configuration=production   # Production build
ng generate component features/x      # Generate component
```

## 14. Implementation Checklist

- [ ] Create Angular project
- [ ] Install dependencies
- [ ] Configure Material theme
- [ ] Create data models
- [ ] Implement API services
- [ ] Create layout components
- [ ] Build dashboard with charts
- [ ] Build categories management
- [ ] Add error handling
- [ ] Add responsive styles
- [ ] Configure proxy for dev
- [ ] Test with backend

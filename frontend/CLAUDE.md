# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
# Install dependencies
npm install

# Start development server (http://localhost:4200)
npm start

# Build for production
npm run build

# Run unit tests (Karma/Jasmine)
npm test

# Generate new component
ng generate component features/<component-name>

# Generate new service
ng generate service core/services/<service-name>
```

## Architecture Overview

This is an Angular 17 standalone components application that tracks job market trends by fetching job posting counts from an external API.

### Project Structure

```
src/app/
├── core/
│   ├── models/          # TypeScript interfaces (Category, JobCountRecord, DashboardSummary, etc.)
│   └── services/        # API layer services (ApiService, CategoryService, StatsService)
├── features/
│   ├── dashboard/
│   │   ├── dashboard.component.ts   # Main dashboard with charts and stats
│   │   └── category-colors.ts       # Neon color config per category
│   └── categories/      # Category management CRUD
└── app.component.ts     # Root component with Material sidenav layout
```

### Key Patterns

- **Standalone Components**: All components use `standalone: true` with direct imports (no NgModules)
- **Lazy Loading**: Feature components are lazy-loaded via dynamic imports in `app.routes.ts`
- **Service Layer**: `ApiService` wraps HttpClient with async/await pattern; domain services (`CategoryService`, `StatsService`) use ApiService
- **State**: Component-local state with Maps and arrays, no external state management library

### Backend Integration

- API base URL configured in `src/environments/environment.ts` (default: `http://localhost:8080`)
- Endpoints:
  - `GET/POST/DELETE /api/categories` - Category CRUD
  - `GET /api/stats/{categorySlug}` - Historical job count records
  - `GET /api/stats/{categorySlug}/latest` - Latest count with change percentage

### UI Stack

- Angular Material for components
- ng2-charts with Chart.js for trend visualization
- SCSS styling with inline component styles
- **Dark theme** with glassmorphism effects
- Inter font (Google Fonts)

### Dark Theme & Styling

The app uses a custom dark theme defined in `src/styles.scss` with CSS custom properties:

```scss
--bg-primary: #0f0f23;      // Main background
--bg-secondary: #1a1a2e;    // Toolbar/sidenav
--bg-card: rgba(30, 30, 50, 0.7);  // Glassmorphism cards
--text-primary: #ffffff;
--text-secondary: rgba(255, 255, 255, 0.7);
```

Category-specific colors are defined in `features/dashboard/category-colors.ts`:
- Java: `#00d4ff` (cyan)
- Python: `#ffd700` (gold)
- DevOps: `#ff6b35` (orange)
- Data: `#a855f7` (purple)
- AI: `#00ff88` (green)
- Testing: `#ff3366` (pink)

### Dashboard Features

The dashboard (`features/dashboard/dashboard.component.ts`) displays a **single category at a time** with the following components:

- **Category selector**: Toggle buttons in the header to switch between active categories (e.g., Java, Data)
- **Hero stats section**: Large job count display with trend indicator (percentage change vs previous)
- **Stats row**: Data points count, min/max/average values for the selected category
- **Date range picker**: Filter data by start/end dates (uses backend `?startDate=&endDate=`)
- **Line chart**: Historical trends for the selected category with category-specific color and gradient fill

State management uses Angular signals (`signal<T>()`) and computed signals for reactive updates:
- `selectedCategory` - currently selected category slug
- `currentStats` - computed stats for selected category
- `activeCategories` - computed list of active categories

### Dependencies Note

- **ng2-charts**: Must use v5.x for Angular 17 (v8+ requires Angular 21+)
- **@angular/cdk**: Must be explicitly added as dependency alongside @angular/material
- **Chart.js**: Must be registered globally in `main.ts` for ng2-charts v5:
  ```typescript
  import { Chart, registerables } from 'chart.js';
  Chart.register(...registerables);
  ```

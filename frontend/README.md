# Job Market Frontend

Angular 17 dashboard for visualizing job market trends.

## Quick Start

```bash
# Install dependencies
npm install

# Start dev server (http://localhost:4200)
npm start

# Build for production
npm run build

# Run tests
npm test
```

## Tech Stack

- **Angular 17** - Standalone components, signals
- **Angular Material** - UI components
- **ng2-charts + Chart.js** - Trend visualization
- **SCSS** - Dark theme with glassmorphism

## Project Structure

```
src/app/
├── core/
│   ├── models/       # TypeScript interfaces
│   └── services/     # API services
├── features/
│   ├── dashboard/    # Main dashboard with charts
│   └── categories/   # Category management
└── app.component.ts  # Root layout
```

## Configuration

API base URL is configured in `src/environments/environment.ts`:

```typescript
export const environment = {
  apiUrl: 'http://localhost:8080'
};
```

## Detailed Documentation

See [`CLAUDE.md`](CLAUDE.md) for comprehensive documentation including:

- Architecture patterns
- Component details
- Dark theme CSS variables
- Category color mappings
- Dependencies compatibility notes

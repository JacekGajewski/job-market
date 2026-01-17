# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Quick Start Commands

```bash
# Full stack with Docker (recommended)
make dev                    # Start dev environment with hot reload
make stop                   # Stop all containers
make logs                   # Follow container logs

# Local development (without Docker)
make run-backend            # Start backend locally (H2 in-memory)
make run-frontend           # Start frontend locally

# Build and test
make build                  # Build both backend and frontend
make test                   # Run all tests
```

See `backend/CLAUDE.md` and `frontend/CLAUDE.md` for module-specific commands.

## Architecture Overview

Monorepo application tracking job market trends from justjoin.it.

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Angular 17    │────▶│  Spring Boot    │────▶│   PostgreSQL    │
│  localhost:4200 │◀────│  localhost:8080 │◀────│  (Docker)       │
└─────────────────┘     └────────┬────────┘     └─────────────────┘
                                 │
                                 ▼
                        ┌─────────────────┐
                        │  justjoin.it    │
                        │  (Daily Scrape) │
                        └─────────────────┘
```

### Data Flow
1. `JobCountScheduler` triggers daily (6 AM, configurable)
2. `JustJoinItScraperService` fetches counts for all active categories and cities
3. Records saved to `job_count_record` table with filter dimensions
4. Frontend fetches via REST API with filter parameters and renders charts

### Key Modules
- **backend/** - Spring Boot 3.4, Java 21, JPA, Flyway migrations
- **frontend/** - Angular 17 standalone components, Material, ng2-charts, dark theme UI
- **docker/** - Compose files for PostgreSQL, backend, frontend, Adminer

### Tracked Categories
The dashboard displays one category at a time with a selector to switch between active categories.

Currently active categories:
- **Java** (slug: `java`) - cyan accent
- **Data** (slug: `data`) - purple accent

Inactive categories (can be reactivated via API):
- **Python** (slug: `python`) - gold accent
- **DevOps** (slug: `devops`) - orange accent
- **AI** (slug: `ai`) - green accent
- **Testing** (slug: `testing`) - pink accent

### Tracked Cities
Cities can be activated to scrape city-specific job counts.

Active cities:
- **Wrocław** (slug: `wroclaw`)
- **Śląsk** (slug: `slask`)

### Filter Dimensions
The dashboard supports multi-dimensional filtering:
- **City**: Filter by location (e.g., Wrocław, Śląsk) or "All Locations"
- **Experience Level**: Junior, Mid, Senior, or All Levels
- **Salary Range**: < 25k, 25-30k, > 30k, or Any Salary
- **Metric Type**: Total, With Salary, Remote, Remote + Salary

This results in **192 unique filter combinations per category**. See [docs/filter-combinations.md](docs/filter-combinations.md) for the complete list.

### Database Profiles
| Profile | Database | Scheduler |
|---------|----------|-----------|
| `dev` | H2 in-memory | disabled |
| `docker` | PostgreSQL | enabled |
| `prod` | PostgreSQL | enabled |

### Environment Variables (.env)
```
POSTGRES_DB=jobmarket
POSTGRES_USER=jobmarket
POSTGRES_PASSWORD=jobmarket123
```

## API Endpoints

### Categories
- `GET /api/categories` - List all tracked categories
- `POST /api/categories` - Create category
- `DELETE /api/categories/{id}` - Remove category
- `PATCH /api/categories/{id}/active?active=true|false` - Set category active status

### Cities
- `GET /api/cities` - List all tracked cities
- `POST /api/cities` - Create city
- `DELETE /api/cities/{id}` - Remove city
- `PATCH /api/cities/{id}/active?active=true|false` - Set city active status

### Statistics
- `GET /api/stats/{slug}` - Historical job counts
  - Query params: `metricType`, `city`, `experienceLevel`, `salaryRange`, `startDate`, `endDate`
- `GET /api/stats/{slug}/latest` - Latest count with change percentage
  - Query params: `metricType`, `city`, `experienceLevel`, `salaryRange`
- `POST /api/stats/scrape` - Manually trigger job count scrape for all active categories and cities

### Documentation
- `GET /swagger-ui.html` - OpenAPI documentation

## Development URLs

| Service | Dev (Docker) | Local |
|---------|--------------|-------|
| Frontend | http://localhost:4200 | http://localhost:4200 |
| Backend | http://localhost:8080 | http://localhost:8080 |
| Adminer | http://localhost:8081 | N/A |
| Debug | localhost:5005 | N/A |

## Java Version

Requires Java 21. If not default:
```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home ./gradlew build
```

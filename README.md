# Job Market Tracker

A monorepo application for tracking job market trends from [justjoin.it](https://justjoin.it). Scrapes daily job counts across multiple categories, cities, experience levels, and salary ranges, then visualizes trends via an interactive dashboard.

## Features

- **Daily Job Scraping** - Automated scraping of job counts from justjoin.it
- **Multi-dimensional Filtering** - Filter by category, city, experience level, salary range, and metric type
- **Trend Visualization** - Interactive charts showing historical job count trends
- **Category Management** - Add/remove tracked job categories via API
- **City Tracking** - Track job counts for specific cities (Wrocław, Śląsk)
- **Dark Theme UI** - Modern glassmorphism design with category-specific accent colors

## Tech Stack

| Layer | Technology |
|-------|------------|
| Frontend | Angular 17, Material, ng2-charts, Chart.js |
| Backend | Spring Boot 3.4, Java 21, JPA, Flyway |
| Database | PostgreSQL (Docker) / H2 (dev) |
| Infrastructure | Docker, Docker Compose, Nginx |

## Prerequisites

- **Java 21** - Required for backend
- **Node.js 18+** - Required for frontend
- **Docker & Docker Compose** - Required for containerized deployment

## Quick Start

### Using Docker (Recommended)

```bash
# Clone and setup
cp .env.example .env

# Start development environment with hot reload
make dev

# View logs
make logs

# Stop all containers
make stop
```

### Local Development

```bash
# Backend (uses H2 in-memory database)
make run-backend

# Frontend (separate terminal)
make run-frontend
```

### Access Points

| Service | URL |
|---------|-----|
| Frontend | http://localhost:4200 |
| Backend API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Adminer | http://localhost:8081 (Docker only) |

## Project Structure

```
job-market/
├── backend/           # Spring Boot API
├── frontend/          # Angular dashboard
├── docker/            # Docker Compose files
├── docs/              # Additional documentation
├── Makefile           # Build/run commands
└── CLAUDE.md          # Detailed project documentation
```

## Documentation

For detailed documentation, see:

- [`CLAUDE.md`](CLAUDE.md) - Project architecture, API endpoints, configuration
- [`backend/CLAUDE.md`](backend/CLAUDE.md) - Backend development guide
- [`frontend/CLAUDE.md`](frontend/CLAUDE.md) - Frontend development guide
- [`docs/filter-combinations.md`](docs/filter-combinations.md) - Filter combinations reference

## API Overview

| Endpoint | Description |
|----------|-------------|
| `GET /api/categories` | List tracked categories |
| `GET /api/cities` | List tracked cities |
| `GET /api/stats/{slug}` | Historical job counts with filters |
| `GET /api/stats/{slug}/latest` | Latest count with trend |
| `POST /api/stats/scrape` | Trigger manual scrape |

## Currently Tracked

**Categories:** Java, Data (Python, DevOps, AI, Testing available but inactive)

**Cities:** Wrocław, Śląsk

**Metrics:** Total, With Salary, Remote, Remote + Salary

## License

Private project - All rights reserved.

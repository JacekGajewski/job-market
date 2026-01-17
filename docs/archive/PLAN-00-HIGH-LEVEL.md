# Job Market - High-Level Architecture Plan

## Overview
A full-stack monorepo application to track job offer trends from justjoin.it over time.

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Angular UI    │────▶│  Spring Boot    │────▶│   PostgreSQL    │
│  (Material +    │◀────│  REST API       │◀────│   Database      │
│   Charts)       │     └────────┬────────┘     └─────────────────┘
└─────────────────┘              │
                                 ▼
                        ┌─────────────────┐
                        │  justjoin.it    │
                        │  (Daily Fetch)  │
                        └─────────────────┘
```

## Key Decisions
- **Fetch Frequency**: Daily
- **Java Version**: 21 (LTS with virtual threads)
- **UI Framework**: Angular Material
- **Repo Structure**: Monorepo (backend/ + frontend/)

---

## Backend (Spring Boot + Java 21)

### Core Components
1. **Scheduler Service** - Daily job count fetching (Spring @Scheduled)
2. **Scraper Service** - Fetch and parse job counts from justjoin.it
3. **REST API** - Expose historical data to frontend
4. **JPA Entities** - Job count records with timestamps

### Key Entities
```
JobCountRecord
├── id (Long)
├── category (String) - e.g., "java", "python", "angular"
├── count (Integer) - number of open positions
├── fetchedAt (LocalDateTime)
└── location (String) - e.g., "all-locations"

TrackedCategory
├── id (Long)
├── name (String) - e.g., "java"
├── slug (String) - URL slug for justjoin.it
└── active (Boolean)
```

### API Endpoints
- `GET /api/categories` - List tracked categories
- `GET /api/stats/{category}` - Historical data for a category
- `GET /api/stats/{category}/latest` - Most recent count
- `POST /api/categories` - Add new category to track
- `DELETE /api/categories/{id}` - Remove category

### Tech Stack
- Java 21
- Spring Boot 3.x
- Spring Data JPA
- Spring Scheduler
- JSoup (HTML parsing) or WebClient (if API available)
- PostgreSQL (prod) / H2 (dev)
- Gradle

---

## Frontend (Angular + Material)

### Core Components
1. **Dashboard** - Main view with trend charts
2. **Category Management** - Add/remove tracked technologies
3. **Chart Components** - Line charts (ng2-charts) showing trends

### Tech Stack
- Angular 17+
- Angular Material
- ng2-charts (Chart.js wrapper)

---

## Project Structure
```
job-market/
├── backend/
│   ├── src/main/java/...
│   ├── src/main/resources/
│   ├── build.gradle
│   └── ...
├── frontend/
│   ├── src/app/
│   ├── angular.json
│   └── ...
└── README.md
```

---

## Data Flow
1. Spring Scheduler triggers daily (configurable time)
2. For each tracked category, fetch justjoin.it page
3. Parse job count from page HTML
4. Save JobCountRecord to database with timestamp
5. Frontend calls REST API to get historical data
6. Angular Material dashboard displays Chart.js line charts

---

## Detailed Plans
1. [Backend Setup Plan](./PLAN-01-BACKEND-SETUP.md)
2. [Scraper Plan](./PLAN-02-SCRAPER.md)
3. [API Plan](./PLAN-03-API.md)
4. [Frontend Plan](./PLAN-04-FRONTEND.md)
5. [Integration Plan](./PLAN-05-INTEGRATION.md)

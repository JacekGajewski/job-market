# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.jobmarket.service.CategoryServiceTest"

# Run the application (uses dev profile with H2 in-memory database)
./gradlew bootRun

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=docker'

# Clean and rebuild
./gradlew clean build
```

**Java Version**: Requires Java 21. Use `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home ./gradlew build` if needed.

## Architecture Overview

This is a Spring Boot 3.4 application that tracks job market statistics by scraping job counts from JustJoinIt and exposing them via REST API.

### Core Components

**Domain Layer** (`com.jobmarket.entity`)
- `TrackedCategory` - Categories to track (e.g., "java", "python"); has name, slug, and active flag
- `TrackedCity` - Cities to track (e.g., "wroclaw", "slask"); has name, slug, and active flag
- `JobCountRecord` - Historical job count data with category, count, fetchedAt, location, city, experienceLevel, salaryMin, salaryMax
- `MetricType` - Enum: TOTAL, WITH_SALARY, REMOTE, REMOTE_WITH_SALARY
- `ExperienceLevel` - Enum: JUNIOR, MID, SENIOR
- `SalaryRange` - Enum: UNDER_25K, RANGE_25_30K, OVER_30K

**API Layer** (`com.jobmarket.controller`)
- `CategoryController` - CRUD for tracked categories (`/api/categories`)
- `CityController` - CRUD for tracked cities (`/api/cities`)
- `StatsController` - Historical stats and latest counts (`/api/stats/{category}`)

**Scraper Module** (`com.jobmarket.scraper`)
- `JustJoinItScraperService` - Orchestrates scraping for all active categories and cities
- `JustJoinItApiClient` - WebFlux client for JustJoinIt API (`/api/offers`)
- `JustJoinItHtmlParser` - JSoup-based HTML parser extracting job counts from page title/meta/elements
- `AnomalyDetectionService` - Detects stale CDN responses by comparing with previous day's count

**Scheduler** (`com.jobmarket.scheduler`)
- `JobCountScheduler` - Cron-based job that fetches and saves counts daily (6 AM by default, configurable)

### Data Flow

1. Scheduler triggers `JustJoinItScraperService.fetchAndSaveAllJobCounts()`
2. Scraper iterates: all active categories × all metric types × (all-locations + all active cities)
3. Results saved to `job_count_record` table with filter dimensions (city, experienceLevel, salaryMin, salaryMax)
4. Stats API serves historical data with multi-dimensional filtering

### Database

- **Dev**: H2 in-memory (`dev` profile) - scheduler disabled
- **Prod/Docker**: PostgreSQL (`prod`/`docker` profiles)
- **Migrations**: Flyway in `src/main/resources/db/migration/`
- **DDL**: `hibernate.ddl-auto=validate` - schema managed by Flyway only

### Key Configuration

Configuration is in `application.yml` with profile-specific overrides:
- `scraper.justjoinit.*` - API/HTML URLs, timeouts, delay between requests
- `scraper.anomaly-detection.*` - Anomaly detection settings (see below)
- `scheduler.job-count.enabled` / `scheduler.job-count.cron` - Scheduler toggle and timing
- `app.cors.allowed-origins` - Frontend CORS origins

### Anomaly Detection

Handles stale CDN responses from justjoin.it by comparing fetched counts with previous day's values.

```yaml
scraper:
  anomaly-detection:
    enabled: true           # Enable/disable anomaly detection
    drop-threshold: 0.20    # 20% drop triggers retry
    retry-delay-ms: 3000    # 3 second wait before retry
    max-retries: 2          # Maximum retry attempts
    minimum-city-count: 5   # Skip check if previous count < 5 (city)
    minimum-global-count: 50 # Skip check if previous count < 50 (all-locations)
```

**How it works:**
1. Fetches job count from justjoin.it
2. Queries previous day's count using `findPreviousByFilters()`
3. If drop > threshold → logs warning, waits, retries
4. Uses higher value (CDN stale data typically returns lower counts)
5. Logs show `source=HTML_RETRY` when retry value was used

**Log examples:**
```
WARN: Anomaly detected for java[TOTAL] city=slask: current=2, previous=230, drop=99.1%
INFO: Retry result for java[TOTAL] city=slask: first=2, retry=230, using=230 (source=HTML_RETRY)
```

### Salary Range Scraping Logic

The JustJoinIt API doesn't reliably support exact salary range queries. To get accurate counts, different salary ranges use different strategies:

| Range | Strategy | Calculation |
|-------|----------|-------------|
| **Any Salary** | Direct query | No salary param |
| **< 25k** | Subtraction | `Total - (>=25k)` |
| **25-30k** | Subtraction | `(>=25k) - (>=30k)` |
| **> 30k** | Direct query | `salary=30000,500000` |

**Why subtraction?**
- Direct query `salary=25000,30000` returns incorrect results (same as total)
- Queries like `salary=25000,500000` (>=25k) work reliably
- Subtracting reliable queries gives accurate range counts

**Implementation:**
- `SalaryRange.java` - `requiresSubtraction=true` for UNDER_25K and RANGE_25_30K
- `JustJoinItScraperService.java`:
  - `fetchCountWithTotalSubtraction()` - for <25k: `total - (>=25k)`
  - `fetchCountWithRangeSubtraction()` - for 25-30k: `(>=25k) - (>=30k)`

**Verification:** Sum of all salary ranges should equal total (any salary):
```
< 25k + 25-30k + > 30k = Total
```

**Cleanup query** (if old incorrect data exists):
```bash
docker exec -it job-market-db psql -U jobmarket -d jobmarket -c "DELETE FROM job_count_record WHERE (salary_min IS NULL AND salary_max = 25000) OR (salary_min = 25000 AND salary_max = 30000);"
```

### Docker

Multi-stage Dockerfile with `development` and `production` targets:
```bash
# Build production image
docker build --target production -t job-market-backend .

# Run development with hot reload
docker build --target development -t job-market-backend-dev .
```

## API Endpoints

### Categories
- `GET /api/categories` - List all tracked categories
- `POST /api/categories` - Create category (body: `{ "name": "...", "slug": "..." }`)
- `DELETE /api/categories/{id}` - Remove category
- `PATCH /api/categories/{id}/active?active=true|false` - Set category active status

### Cities
- `GET /api/cities` - List all tracked cities
- `POST /api/cities` - Create city (body: `{ "name": "...", "slug": "..." }`)
- `DELETE /api/cities/{id}` - Remove city
- `PATCH /api/cities/{id}/active?active=true|false` - Set city active status

### Statistics
- `GET /api/stats/{category}` - Historical job counts
  - Query params: `metricType`, `city`, `experienceLevel`, `salaryRange`, `startDate`, `endDate`
- `GET /api/stats/{category}/latest` - Latest count with change from previous
  - Query params: `metricType`, `city`, `experienceLevel`, `salaryRange`
- `POST /api/stats/scrape` - Manually trigger job count scrape for all active categories and cities

### Other
- `GET /actuator/health` - Health check

OpenAPI/Swagger UI available at `/swagger-ui.html`

### JustJoinIt Scraper Notes

- The JustJoinIt API (`/api/offers`) currently returns 404 - HTML parsing is the primary working method
- Valid category slugs: `java`, `python`, `javascript`, `devops`, `data`
- Invalid slugs (return 404): `typescript`, `angular`, `react`, `spring`, `node`
- Job counts in HTML use non-breaking spaces as thousand separators (e.g., "1 422")

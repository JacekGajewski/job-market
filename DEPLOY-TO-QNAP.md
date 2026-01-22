# Deploy job-market to QNAP NAS

## Overview
Build Docker images directly on the QNAP NAS (native AMD64) and run with Docker Compose.

## Prerequisites
- QNAP NAS with Container Station installed
- SSH access to NAS
- Docker Hub account (optional, for pushing images)

---

## Quick Deploy (if images already built)

```bash
ssh dupsram@192.168.50.238
cd /share/CACHEDEV1_DATA/projects
docker compose -f docker-compose.nas.yml up -d
```

---

## Full Deployment Steps

### Step 1: Copy Source Code to NAS

From your Mac:
```bash
cd /Users/gajewskij/Projects/job-market
rsync -av --exclude 'node_modules' --exclude '.git' --exclude 'build' --exclude 'target' --exclude '.gradle' . dupsram@192.168.50.238:/share/CACHEDEV1_DATA/projects/job-market-src/
```

### Step 2: Build Images on NAS

SSH into NAS and build (native AMD64, no emulation issues):
```bash
ssh dupsram@192.168.50.238
cd /share/CACHEDEV1_DATA/projects/job-market-src
docker build -t tnt9/job-market-backend:latest ./backend
docker build -t tnt9/job-market-frontend:latest ./frontend
```

### Step 3: Deploy with Docker Compose

```bash
cd /share/CACHEDEV1_DATA/projects
docker compose -f docker-compose.nas.yml up -d
```

### Step 4: Verify Deployment

```bash
docker ps
```

Should show 3 containers running:
- `job-market-frontend` (port 80)
- `job-market-backend` (port 8080)
- `job-market-db` (port 5432)

---

## Access URLs

| Service | URL |
|---------|-----|
| Frontend | http://192.168.50.238 |
| Backend API | http://192.168.50.238:8080 |
| Swagger Docs | http://192.168.50.238:8080/swagger-ui.html |

---

## Docker Compose File (docker-compose.nas.yml)

Located at `/share/CACHEDEV1_DATA/projects/docker-compose.nas.yml`:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: job-market-db
    environment:
      POSTGRES_DB: jobmarket
      POSTGRES_USER: jobmarket
      POSTGRES_PASSWORD: jobmarket123
    ports:
      - "5432:5432"
    volumes:
      - /share/CACHEDEV1_DATA/projects/job-market-data/postgres:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U jobmarket"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - job-market-network

  backend:
    image: tnt9/job-market-backend:latest
    container_name: job-market-backend
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/jobmarket
      SPRING_DATASOURCE_USERNAME: jobmarket
      SPRING_DATASOURCE_PASSWORD: jobmarket123
      JAVA_OPTS: -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - job-market-network

  frontend:
    image: tnt9/job-market-frontend:latest
    container_name: job-market-frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - job-market-network

networks:
  job-market-network:
    driver: bridge
```

---

## Common Commands

### View logs
```bash
docker compose -f docker-compose.nas.yml logs -f
docker logs job-market-backend
docker logs job-market-frontend
```

### Restart services
```bash
docker compose -f docker-compose.nas.yml restart
```

### Stop all services
```bash
docker compose -f docker-compose.nas.yml down
```

### Rebuild and redeploy
```bash
# On Mac - sync updated source
rsync -av --exclude 'node_modules' --exclude '.git' --exclude 'build' --exclude 'target' --exclude '.gradle' . dupsram@192.168.50.238:/share/CACHEDEV1_DATA/projects/job-market-src/

# On NAS - rebuild and restart
cd /share/CACHEDEV1_DATA/projects/job-market-src
docker build -t tnt9/job-market-backend:latest ./backend
docker build -t tnt9/job-market-frontend:latest ./frontend
cd /share/CACHEDEV1_DATA/projects
docker compose -f docker-compose.nas.yml up -d
```

---

## Troubleshooting

### Container Station Storage Issue
If you get "no space left on device" errors, Container Station may be using wrong storage:

1. Check where Docker stores data:
   ```bash
   df /share/Container/
   ```
   Should show your data volume, NOT tmpfs

2. If showing tmpfs, reinstall Container Station:
   - Remove Container Station from App Center
   - Remove the Container shared folder
   - Reinstall and select a volume with sufficient space (e.g., Plex volume)

### Platform Mismatch Error
If you see "platform (linux/arm64) does not match host (linux/amd64)":
- Images were built on Mac (ARM64)
- Rebuild images directly on NAS (AMD64)

### Check container health
```bash
docker ps
docker inspect job-market-backend | grep -A 10 Health
```

---

## NAS Details

- **IP**: 192.168.50.238
- **User**: dupsram
- **Data Volume**: /share/CACHEDEV1_DATA (Backups) or /share/CACHEDEV2_DATA (Plex)
- **Container Station Storage**: PlexDatabase folder on Plex volume

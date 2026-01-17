# Deploy job-market to QNAP NAS via Docker Hub

## Overview
Build Docker images locally on Mac, push to Docker Hub, then deploy on QNAP Container Station using pre-built images.

## Prerequisites
- Docker Hub account (free at https://hub.docker.com)
- Docker Desktop running on your Mac

---

## Step 1: Login to Docker Hub on Mac

```bash
docker login
```
Enter your Docker Hub username and password when prompted.

---

## Step 2: Build the images locally

```bash
cd /Users/gajewskij/Projects/job-market/docker
docker compose build
```

This builds `backend` and `frontend` images from the Dockerfiles.

---

## Step 3: Tag images for Docker Hub

```bash
docker tag docker-backend:latest tnt9/job-market-backend:latest
docker tag docker-frontend:latest tnt9/job-market-frontend:latest
```

> Note: The image names might be `job-market-docker-backend` or similar. Check with `docker images` to see the exact names after building.

---

## Step 4: Push images to Docker Hub

```bash
docker push tnt9/job-market-backend:latest
docker push tnt9/job-market-frontend:latest
```

---

## Step 5: Create NAS-specific docker-compose file

Create a new file `docker/docker-compose.nas.yml`:

```yaml
version: '3.9'

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

**Note**: Images are configured to pull from Docker Hub account `tnt9`.

---

## Step 6: Create data directory on NAS

```bash
ssh dupsram@192.168.50.238 "mkdir -p /share/CACHEDEV1_DATA/projects/job-market-data/postgres"
```

---

## Step 7: Copy the NAS compose file to QNAP

```bash
scp -O /Users/gajewskij/Projects/job-market/docker/docker-compose.nas.yml dupsram@192.168.50.238:/share/CACHEDEV1_DATA/projects/
```

---

## Step 8: Deploy in Container Station

1. Open **Container Station** on your QNAP
2. Go to **Applications** in the left menu
3. Click **Create** button
4. Select **"Create Application"**
5. Either:
   - **Browse** to `/share/CACHEDEV1_DATA/projects/` and select `docker-compose.nas.yml`
   - Or **paste** the contents of the NAS compose file
6. Set application name: `job-market`
7. Click **Create**

Container Station will pull the images from Docker Hub and start the containers.

---

## Step 9: Verify deployment

After deployment, access:
- **Frontend**: http://192.168.50.238:80
- **Backend API**: http://192.168.50.238:8080

Check container status in Container Station -> Containers.

---

## Updating the application later

When you make code changes:

1. Rebuild on Mac: `docker compose build`
2. Tag new images: `docker tag ...`
3. Push to Docker Hub: `docker push ...`
4. On QNAP Container Station: Stop the application, then Start again (it will pull the latest images)

Or use SSH:
```bash
ssh dupsram@192.168.50.238
cd /share/CACHEDEV1_DATA/projects
docker compose -f docker-compose.nas.yml pull
docker compose -f docker-compose.nas.yml up -d
```

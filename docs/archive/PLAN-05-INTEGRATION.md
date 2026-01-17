# Integration and Docker Setup Plan

## Overview
Docker containerization for local development and production deployment.

## 1. Directory Structure

```
job-market/
├── backend/
│   └── Dockerfile
├── frontend/
│   ├── Dockerfile
│   └── nginx/nginx.conf
├── docker/
│   ├── docker-compose.yml
│   ├── docker-compose.dev.yml
│   └── docker-compose.prod.yml
├── .env.example
└── Makefile
```

## 2. Docker Compose (Main)

### docker-compose.yml
```yaml
version: '3.9'

services:
  postgres:
    image: postgres:16-alpine
    container_name: job-market-db
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-jobmarket}
      POSTGRES_USER: ${POSTGRES_USER:-jobmarket}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-jobmarket123}
    ports:
      - "${POSTGRES_PORT:-5432}:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U jobmarket"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - job-market-network

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
      target: production
    container_name: job-market-backend
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-jobmarket}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-jobmarket}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-jobmarket123}
      JAVA_OPTS: -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
    ports:
      - "${BACKEND_PORT:-8080}:8080"
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - job-market-network

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
      target: production
    container_name: job-market-frontend
    ports:
      - "${FRONTEND_PORT:-4200}:80"
    depends_on:
      - backend
    networks:
      - job-market-network

volumes:
  postgres_data:

networks:
  job-market-network:
    driver: bridge
```

## 3. Backend Dockerfile

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew gradle build.gradle settings.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Development stage
FROM eclipse-temurin:21-jdk-alpine AS development
WORKDIR /app
COPY --from=builder /app/gradlew /app/gradle /app/build.gradle /app/settings.gradle ./
CMD ["./gradlew", "bootRun", "--no-daemon"]

# Production stage
FROM eclipse-temurin:21-jre-alpine AS production
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=builder /app/build/libs/*.jar app.jar
RUN chown -R app:app /app
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

## 4. Frontend Dockerfile

```dockerfile
# Build stage
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration=production

# Development stage
FROM node:20-alpine AS development
WORKDIR /app
COPY package*.json ./
RUN npm ci
EXPOSE 4200
CMD ["npm", "start", "--", "--host", "0.0.0.0"]

# Production stage
FROM nginx:alpine AS production
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx/nginx.conf /etc/nginx/conf.d/
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## 5. Nginx Configuration

### frontend/nginx/nginx.conf
```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    gzip on;
    gzip_types text/plain text/css application/json application/javascript;

    # API proxy
    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Actuator proxy
    location /actuator/ {
        proxy_pass http://backend:8080/actuator/;
    }

    # Angular SPA routing
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

## 6. Environment Template

### .env.example
```bash
# Database
POSTGRES_DB=jobmarket
POSTGRES_USER=jobmarket
POSTGRES_PASSWORD=your_secure_password

# Ports
POSTGRES_PORT=5432
BACKEND_PORT=8080
FRONTEND_PORT=4200

# Spring
SPRING_PROFILES_ACTIVE=docker
```

## 7. Development Overrides

### docker-compose.dev.yml
```yaml
version: '3.9'

services:
  backend:
    build:
      target: development
    volumes:
      - ./backend/src:/app/src:ro
    environment:
      JAVA_OPTS: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
    ports:
      - "8080:8080"
      - "5005:5005"

  frontend:
    build:
      target: development
    volumes:
      - ./frontend/src:/app/src:ro
    ports:
      - "4200:4200"
    command: ["npm", "start", "--", "--host", "0.0.0.0", "--poll", "1000"]

  adminer:
    image: adminer:latest
    ports:
      - "8081:8080"
    depends_on:
      - postgres
    networks:
      - job-market-network
```

## 8. Makefile

```makefile
.PHONY: dev prod stop logs clean

dev:
	docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build

prod:
	docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d

stop:
	docker compose down

logs:
	docker compose logs -f

clean:
	docker compose down -v --remove-orphans

db-shell:
	docker compose exec postgres psql -U jobmarket -d jobmarket
```

## 9. Development Workflow

```bash
# First time setup
cp .env.example .env
make dev

# Access services
# Frontend: http://localhost:4200
# Backend:  http://localhost:8080/api
# Adminer:  http://localhost:8081
# Debug:    localhost:5005

# Stop
make stop
```

## 10. Health Checks

### Backend (Spring Actuator)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when-authorized
```

## 11. CORS Configuration

### Backend WebConfig.java
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:4200", "http://frontend:80")
            .allowedMethods("GET", "POST", "DELETE")
            .allowCredentials(true);
    }
}
```

## 12. Implementation Checklist

- [ ] Create docker-compose.yml
- [ ] Create docker-compose.dev.yml
- [ ] Create backend Dockerfile
- [ ] Create frontend Dockerfile
- [ ] Create nginx.conf
- [ ] Create .env.example
- [ ] Create Makefile
- [ ] Add Spring Actuator
- [ ] Configure CORS
- [ ] Test full stack locally
- [ ] Document in README

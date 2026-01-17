.PHONY: dev prod stop logs clean build test db-shell help

# Default target
help:
	@echo "Job Market - Available Commands:"
	@echo ""
	@echo "  make dev       - Start development environment (with hot reload)"
	@echo "  make prod      - Start production environment"
	@echo "  make stop      - Stop all containers"
	@echo "  make logs      - Follow container logs"
	@echo "  make clean     - Stop and remove all containers, volumes"
	@echo "  make build     - Build backend and frontend"
	@echo "  make test      - Run all tests"
	@echo "  make db-shell  - Open PostgreSQL shell"
	@echo ""

# Development environment with hot reload
dev:
	@echo "Starting development environment..."
	cd docker && docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build

# Production environment
prod:
	@echo "Starting production environment..."
	cd docker && docker compose -f docker-compose.yml up --build -d

# Stop all containers
stop:
	@echo "Stopping containers..."
	cd docker && docker compose down

# Follow logs
logs:
	cd docker && docker compose logs -f

# Clean everything (including volumes)
clean:
	@echo "Cleaning up..."
	cd docker && docker compose down -v --remove-orphans
	rm -rf backend/build frontend/dist

# Build backend
build-backend:
	@echo "Building backend..."
	cd backend && JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home ./gradlew build -x test

# Build frontend
build-frontend:
	@echo "Building frontend..."
	cd frontend && npm run build

# Build both
build: build-backend build-frontend

# Run backend tests
test-backend:
	@echo "Running backend tests..."
	cd backend && JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home ./gradlew test

# Run frontend tests
test-frontend:
	@echo "Running frontend tests..."
	cd frontend && npm test -- --watch=false

# Run all tests
test: test-backend test-frontend

# Open database shell
db-shell:
	cd docker && docker compose exec postgres psql -U jobmarket -d jobmarket

# Backend local run (without Docker)
run-backend:
	@echo "Starting backend locally..."
	cd backend && JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home ./gradlew bootRun

# Frontend local run (without Docker)
run-frontend:
	@echo "Starting frontend locally..."
	cd frontend && npm start

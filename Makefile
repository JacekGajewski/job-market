.PHONY: dev prod stop logs clean build test db-shell help \
	deploy-nas sync-nas build-nas restart-nas logs-nas stop-nas

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
	@echo "NAS Deployment (QNAP):"
	@echo "  make deploy-nas  - Full deploy: sync, build, restart"
	@echo "  make sync-nas    - Sync source code to NAS"
	@echo "  make build-nas   - Build images on NAS"
	@echo "  make restart-nas - Restart containers on NAS"
	@echo "  make logs-nas    - View NAS container logs"
	@echo "  make stop-nas    - Stop NAS containers"
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

# =============================================================================
# NAS Deployment (QNAP)
# =============================================================================

NAS_HOST := dupsram@192.168.50.238
NAS_SRC := /share/CACHEDEV1_DATA/projects/job-market-src
NAS_DEPLOY := /share/CACHEDEV1_DATA/projects
SSH_CTRL := /tmp/ssh-nas-$(shell echo $$PPID)

# SSH with connection multiplexing (reuses single connection)
SSH_CMD = ssh -t -o ControlPath=$(SSH_CTRL) $(NAS_HOST)

# Start SSH master connection (password once)
ssh-nas-connect:
	@echo "Connecting to NAS (enter password once)..."
	@ssh -o ControlMaster=yes -o ControlPath=$(SSH_CTRL) -o ControlPersist=10m -fN $(NAS_HOST)

# Close SSH master connection
ssh-nas-disconnect:
	@ssh -o ControlPath=$(SSH_CTRL) -O exit $(NAS_HOST) 2>/dev/null || true

# Sync source code to NAS
sync-nas:
	@echo "Syncing source code to NAS..."
	@rsync -av --delete -e "ssh -o ControlPath=$(SSH_CTRL)" \
		--exclude 'node_modules' \
		--exclude '.git' \
		--exclude 'build' \
		--exclude 'target' \
		--exclude '.gradle' \
		--exclude 'dist' \
		. $(NAS_HOST):$(NAS_SRC)/

# Docker path on QNAP Container Station
NAS_DOCKER_PATH := /share/CACHEDEV2_DATA/.qpkg/container-station/bin

# Build images and restart containers on NAS (single SSH session, one sudo password)
build-and-restart-nas:
	@echo "Building and deploying on NAS..."
	@$(SSH_CMD) "export PATH=$(NAS_DOCKER_PATH):\$$PATH && \
		echo '=== Building backend ===' && \
		cd $(NAS_SRC) && sudo -E docker build --progress=plain -t tnt9/job-market-backend:latest ./backend && \
		echo '=== Building frontend ===' && \
		sudo -E docker build --progress=plain -t tnt9/job-market-frontend:latest ./frontend && \
		echo '=== Restarting containers ===' && \
		cd $(NAS_DEPLOY) && sudo -E docker compose -f docker-compose.nas.yml up -d"

# Individual targets for manual use
build-nas:
	@echo "Building images on NAS..."
	@ssh -t $(NAS_HOST) "export PATH=$(NAS_DOCKER_PATH):\$$PATH; cd $(NAS_SRC) && \
		sudo -E docker build --progress=plain -t tnt9/job-market-backend:latest ./backend && \
		sudo -E docker build --progress=plain -t tnt9/job-market-frontend:latest ./frontend"

restart-nas:
	@echo "Restarting containers on NAS..."
	@ssh -t $(NAS_HOST) "export PATH=$(NAS_DOCKER_PATH):\$$PATH; cd $(NAS_DEPLOY) && sudo -E docker compose -f docker-compose.nas.yml up -d"

# Full deploy: sync, build, restart (single sudo password for build+restart)
deploy-nas: ssh-nas-connect sync-nas build-and-restart-nas ssh-nas-disconnect
	@echo ""
	@echo "Deployment complete!"
	@echo "Frontend: http://192.168.50.238"
	@echo "Backend:  http://192.168.50.238:8080"

# View NAS logs
logs-nas:
	@ssh -t $(NAS_HOST) "export PATH=$(NAS_DOCKER_PATH):\$$PATH; cd $(NAS_DEPLOY) && sudo -E docker compose -f docker-compose.nas.yml logs -f"

# Stop NAS containers
stop-nas:
	@echo "Stopping NAS containers..."
	@ssh -t $(NAS_HOST) "export PATH=$(NAS_DOCKER_PATH):\$$PATH; cd $(NAS_DEPLOY) && sudo -E docker compose -f docker-compose.nas.yml down"

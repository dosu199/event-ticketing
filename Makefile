# =====================================================================
# Makefile: short aliases for commands you run all the time.
# `make <target>` from the repo root. `make help` lists the targets.
# Recipe lines MUST start with a TAB character, not spaces.
# =====================================================================

COMPOSE = docker compose -f infra/docker/docker-compose.yml --env-file .env

# .PHONY: these targets are command names, not files to build.
.PHONY: help env up down ps logs reset build test

help: ## List available targets
	@grep -E "^[a-zA-Z_-]+:.*## " $(MAKEFILE_LIST) | awk -F":.*## " '{printf "  %-8s %s\n", $$1, $$2}'

env: ## Create .env from .env.example if it does not exist yet
	@test -f .env || (cp .env.example .env && echo "Created .env")

up: env ## Start all infrastructure containers in the background
	$(COMPOSE) up -d

down: ## Stop and remove containers (data volumes are kept)
	$(COMPOSE) down

ps: ## Show container status and health
	$(COMPOSE) ps

logs: ## Follow logs of all containers (Ctrl+C to stop)
	$(COMPOSE) logs -f

reset: ## DANGER: stop everything and DELETE all data volumes
	$(COMPOSE) down -v

build: ## Compile and package all services, skipping tests
	./mvnw -q package -DskipTests

test: ## Build and run all tests
	./mvnw verify

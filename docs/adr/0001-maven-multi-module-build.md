# 0001: Maven multi-module build

- **Status:** accepted
- **Date:** 2026-10-30

## Context
We have six Spring Boot services that share the same Java version, Spring Boot version, and Spring Cloud version. We need a build tool and a way to keep those versions in sync.

## Options
1. **Maven multi-module:** one root `pom.xml` (the "parent") that lists every service as a module. Each service has its own small `pom.xml` that inherits from the parent.
2. **Gradle multi-project:** same idea with `settings.gradle` and Kotlin/Groovy build scripts.
3. **Fully separate builds:** each service has its own independent build with no shared parent.

## Decision
**Option 1: Maven multi-module.**

## Why
- Maven is the most common build tool in Spring job postings and existing codebases, so it's the most useful to refresh.
- Its XML is verbose, but declarative and predictable: there's no build "code" to debug.
- A shared parent means the Spring Boot version is changed in **one** place.
- From the repo root, `./mvnw verify` builds and tests everything. `./mvnw -pl services/event-service verify` builds one service.

## Consequences
- Easier: consistent versions, one command to build everything, simple CI.
- Harder: services are coupled through the parent POM. Bumping Spring Boot affects every service at once. In a large company, teams often use option 3 so each service upgrades independently. For a learning project, consistency matters more.
- Each service still produces its **own** runnable jar and Docker image, so they stay independently deployable.

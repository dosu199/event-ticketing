# 0002: Spring Boot 4.1 and Java 21

- **Status:** accepted
- **Date:** 2026-10-30

## Context
Earlier planning mentioned "Spring Boot 3.x". By October 2026, Spring Boot 4 is the current generation (4.0 released November 2025, 4.1 in June 2026), and Spring Boot 3.5 is on its way out of open-source support.

## Decision
- **Spring Boot 4.1.1**
- **Spring Cloud 2025.1.2 ("Oakwood")**, the release train compatible with Boot 4.0 and 4.1
- **Java 21 (LTS)**

## Why
- Learning the current version is more valuable than refreshing an outgoing one.
- Java 21 is LTS, widely deployed, and fully supported by Boot 4. Java 25 (the newer LTS) would also work, so switching later only means changing `<java.version>` in the root POM and the devcontainer.

## Things that are different in Boot 4 (know these when reading older tutorials)
| Spring Boot 3 | Spring Boot 4 |
|---|---|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| `spring-boot-starter-oauth2-resource-server` | `spring-boot-starter-security-oauth2-resource-server` |
| `flyway-core` alone was enough | add `spring-boot-starter-flyway` |
| `spring-kafka` | `spring-boot-starter-kafka` |
| Jackson 2 (`com.fasterxml.jackson`) | Jackson 3 (`tools.jackson`) |
| `spring-cloud-starter-gateway` | `spring-cloud-starter-gateway-server-webflux` |

Most tutorials online are still Boot 3. The concepts are identical, and usually only artifact names and package imports differ.

## Consequences
- Some third-party libraries may lag behind Boot 4. Check compatibility before adding one.

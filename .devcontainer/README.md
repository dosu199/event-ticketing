# .devcontainer: the Codespaces environment definition

`devcontainer.json` tells GitHub Codespaces (and VS Code Dev Containers) **how to build your development machine**. Without it, Codespaces uses a generic "universal" image. With it, every rebuild gives the exact same tools, and anyone who opens the repo gets the same setup.

## What each part does

| Key | What it does | Why we need it |
|---|---|---|
| `image` | Base OS: Ubuntu 24.04 with common dev tools (git, curl, zsh) | A small, clean starting point. Everything else is added by features. |
| `features.java` | Installs **Java 21 (Eclipse Temurin)** and **Maven** | Builds and runs the Spring services (ADR 0002) |
| `features.node` | Installs **Node.js 22** with npm | Runs the React/Vite frontend |
| `features.docker-in-docker` | Runs a Docker engine *inside* the Codespace | `docker compose` needs it to start Postgres, Kafka, and the rest |
| `forwardPorts` | Makes these ports reachable from your browser | Opens Keycloak, Mailpit, Kafka UI, and the services through Codespaces URLs |
| `portsAttributes` | Gives each port a readable label in the **Ports** tab | So you see "keycloak" instead of just "8180" |
| `postCreateCommand` | Runs once after the container is created: copies `.env.example` to `.env` (`-n` = don't overwrite) | Docker compose has its variables ready on first start |
| `customizations.vscode.extensions` | Auto-installs editor extensions | Java language support and debugging, Spring Boot tools, Docker/compose UI, YAML validation, `.env` highlighting |

## How to apply it
After this folder is in your repo, open the Command Palette (`Ctrl/Cmd+Shift+P`) and run **Codespaces: Rebuild Container**. The rebuild takes a few minutes the first time.

## Machine size
- **2-core / 8 GB** runs the Phase 0 infrastructure and one or two services.
- **4-core / 16 GB** is recommended once several Spring services run at the same time (from Phase 3 onward).

Change it under *Codespaces → … → Change machine type*. Larger machines use your free monthly core-hours faster, so stop the Codespace when you're done.

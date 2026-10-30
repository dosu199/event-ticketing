# Phase 0: Setup

**Goal:** a repository that builds, a development environment that's identical every time, and all infrastructure (database, cache, broker, identity, email) running with one command. No business logic yet.

**Definition of done**

- [ ] `make up` starts all containers and `make ps` shows them healthy
- [ ] `./mvnw verify` builds all six services and their smoke tests pass
- [ ] Each service starts and answers `GET /actuator/health` with `{"status":"UP"}`
- [ ] You can open Keycloak, Mailpit, and Kafka UI in the browser

---

## 1. What was added, and why

```
event-ticketing/
├── .devcontainer/                 ← Codespaces machine definition
│   ├── devcontainer.json
│   └── README.md
├── .env.example                   ← template for local secrets
├── Makefile                       ← short aliases: make up / down / ps / test
├── pom.xml                        ← Maven parent: versions + module list
├── docs/
│   ├── adr/0001..0004             ← the decisions behind this phase
│   └── phases/phase-0.md          ← this file
├── infra/docker/
│   ├── docker-compose.yml         ← all infrastructure containers
│   └── postgres/init/01-create-service-databases.sh
└── services/<each service>/
    ├── pom.xml                    ← the service's build file
    ├── src/main/java/.../XxxApplication.java
    ├── src/main/resources/application.yml
    └── src/test/java/.../XxxApplicationTests.java
```

Every file also has comments inside explaining each line. This document explains the bigger picture.

---

## 2. The development environment: `.devcontainer/`

**Problem it solves:** "it works on my machine". Without a definition, your Codespace has whatever the default image ships, and versions drift over time.

**What it is:** a recipe for the Codespace. It takes an Ubuntu base image and adds **features** (prepackaged installers): Java 21 + Maven, Node 22, and Docker-in-Docker.

**Docker-in-Docker** deserves a note. A Codespace is itself a container. To run *more* containers (Postgres, Kafka) inside it, it needs its own Docker engine, and that's what this feature provides.

**Port forwarding:** services inside a Codespace aren't on your laptop. Codespaces creates a URL for each forwarded port (shown in the **Ports** tab), for example `https://<name>-8180.app.github.dev` for Keycloak. Inside the Codespace terminal, `localhost:8180` still works.

Details: [`.devcontainer/README.md`](../../.devcontainer/README.md)

---

## 3. The build: Maven multi-module

### 3.1 Root `pom.xml` (the parent)

Think of it as a **contract every service signs**:

| Section | Meaning |
|---|---|
| `<parent>spring-boot-starter-parent 4.1.1` | Inherit Spring Boot's managed versions and plugin defaults. This is why service POMs never write `<version>` on Spring dependencies. |
| `<packaging>pom</packaging>` | This project produces no jar. It only organizes other projects. |
| `<modules>` | The six service folders. Maven builds them in one reactor run. |
| `<java.version>21` | Compiler target for every service |
| `<dependencyManagement>` + Spring Cloud BOM | "If anyone uses a Spring Cloud library, use the 2025.1.2 versions." It doesn't add dependencies by itself. |

**BOM (Bill of Materials):** a POM that only contains a list of compatible versions. Importing it means "trust this tested combination". Spring Boot and Spring Cloud each publish one.

### 3.2 Service `pom.xml`

Each one points `<parent>` to the root POM (`../../pom.xml`) and declares only what it needs:

| Dependency | Why it's here in Phase 0 |
|---|---|
| `spring-boot-starter-webmvc` | Embedded Tomcat + Spring MVC, so the service can serve HTTP. (Gateway uses `spring-cloud-starter-gateway-server-webflux` instead, which runs on Netty.) |
| `spring-boot-starter-actuator` | `/actuator/health`, used to check the service is alive. Docker and Kubernetes rely on this later. |
| `spring-boot-starter-test` (test scope) | JUnit 5, AssertJ, Mockito, and `@SpringBootTest` |

`spring-boot-maven-plugin` turns the build into an **executable fat jar**: one jar containing your code and every library, runnable with `java -jar`.

> **Rule for the rest of the project:** add a dependency in the phase where you first use it, and write one line in that service's README saying why. For example, JPA + PostgreSQL driver + Flyway arrive in Phase 1 for event-service only.

### 3.3 Maven Wrapper (`mvnw`): your first hands-on step

The wrapper is a small script committed to the repo that downloads and runs **a fixed Maven version**, so everyone (and CI) builds with the same Maven, regardless of what's installed. You generate it once:

```bash
mvn -N wrapper:wrapper -Dmaven=3.9.11
```

- `-N` (non-recursive) means "run only on the root project, not each module".
- It creates `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/maven-wrapper.properties`. Commit all three.
- From then on, always use `./mvnw ...` instead of `mvn ...`.

---

## 4. The services' starting code

Each service has the same three files. They're what [start.spring.io](https://start.spring.io) would generate, with comments added.

### `XxxApplication.java`
The `main` method that boots Spring. `@SpringBootApplication` turns on auto-configuration and **component scanning** of its own package and everything below it. That's why it lives in `com.ticketing.<service>`, the parent of `api`, `domain`, `service`, and the other sub-packages.

### `application.yml`
Configuration read at startup:

- `spring.application.name`: the service's identity in logs, metrics, and traces.
- `server.port`: a fixed port per service, so all six can run side by side.
- `management.*`: which Actuator endpoints are exposed over HTTP.

Any property can be overridden by an environment variable: `server.port` becomes `SERVER_PORT`. Containers and Kubernetes configure apps exactly this way later.

The gateway's file also contains a **commented example route** to use in Phase 1.

### `XxxApplicationTests.java`
A **smoke test**: `@SpringBootTest` starts the whole application context, and the test passes if nothing blows up. As you add database or Kafka configuration, this test catches wiring mistakes immediately.

---

## 5. The infrastructure: `infra/docker/docker-compose.yml`

**Docker Compose** describes several containers, their configuration, ports, and storage in one YAML file and starts them together on a shared network. Containers on that network reach each other **by service name** (`postgres`, `kafka`). Your machine reaches them through **published ports** (`localhost:5432`).

| Container | Image | Host port(s) | Role in the project |
|---|---|---|---|
| `postgres` | `postgres:17-alpine` | 5432 | Relational data for event, booking, payment, and ticket services |
| `redis` | `redis:7.4-alpine` | 6379 | Seat holds with TTL (Phase 3), gateway rate limiting |
| `kafka` | `apache/kafka:4.3.2` | 9092 | Event log connecting the services asynchronously (Phase 4) |
| `kafka-ui` | `kafbat/kafka-ui` | 8090 | Web console to see topics and messages |
| `keycloak` | `quay.io/keycloak/keycloak:26.8.1` | 8180 | Login, users, roles, JWTs (Phase 2) |
| `mailpit` | `axllent/mailpit` | 1025 (SMTP), 8025 (UI) | Catches outgoing email (Phase 5) |

### Concepts used in the file

- **`ports: "host:container"`**: maps a port on your machine to one inside the container. Keycloak listens on 8080 *inside*, but is published on **8180** because our gateway owns 8080 on the host.
- **Named volumes** (`pgdata`, `keycloakdata`): Docker-managed storage that survives `docker compose down`. `down -v` (`make reset`) deletes them. Kafka and Redis deliberately have no volume, so their data starts clean on every restart.
- **`healthcheck`**: a command Docker runs periodically to know when a container is *ready*, not just *started*. Postgres takes a few seconds before it accepts connections. `make ps` shows `healthy` once the check passes.
- **`depends_on: condition: service_healthy`**: Kafka UI waits until Kafka is healthy before starting.
- **`${VAR:-default}`**: reads the value from `.env`, or uses the default after `:-` if it isn't set.

### Postgres: the init script
`postgres/init/01-create-service-databases.sh` creates `event_db`/`event_user`, `booking_db`/`booking_user`, and so on, and revokes public access so each user can only connect to its own database. Postgres runs init scripts **only when its data volume is empty**, which means only on the very first start. ADR 0004 explains why we use one container with separate databases.

Try it once things are running:

```bash
docker exec -it et-postgres psql -U postgres -c '\l'        # list databases
docker exec -it et-postgres psql -U event_user -d booking_db # should be DENIED
```

### Kafka: the two-listener setup
This is the part that confuses everyone at first:

1. A client connects to a **bootstrap** address.
2. Kafka answers: "for this partition, talk to me at **<advertised listener>**".
3. The client reconnects to *that* address.

So the advertised address must be reachable **from where the client runs**:

| Client runs… | Uses | Advertised as |
|---|---|---|
| on your machine / in the Codespace terminal (`./mvnw spring-boot:run`) | `localhost:9092` | `EXTERNAL://localhost:9092` |
| inside Docker (Kafka UI now, our services later) | `kafka:29092` | `INTERNAL://kafka:29092` |

`KAFKA_PROCESS_ROLES: broker,controller` means this single node stores messages *and* manages cluster metadata. That's **KRaft mode**, which replaced ZooKeeper (ADR 0003). Auto-creating topics is disabled so a typo in a topic name fails loudly instead of silently creating a new topic.

### Keycloak
It runs in `start-dev` mode: plain HTTP, its own embedded database, and a bootstrap admin user from `.env`. Phase 2 adds a **realm export** under `infra/keycloak/`, so the realm, roles, users, and clients are created automatically.

### Mailpit
A fake mail server. In Phase 5, notification-service sends to `localhost:1025`, and you read the message in the web inbox on port 8025. Nothing ever reaches a real mailbox.

---

## 6. Supporting files

### `.env.example` → `.env`
Compose reads variables (passwords, usernames) from `.env`. The **real** `.env` is git-ignored, and the **example** is committed so everyone knows which variables exist. This is the standard pattern for keeping secrets out of Git. (`make up` and the devcontainer create `.env` for you.)

### `Makefile`
Short names for long commands, and self-documenting: `make help`.

| Target | Runs |
|---|---|
| `make up` | create `.env` if missing, then `docker compose … up -d` |
| `make ps` | container status and health |
| `make logs` | follow all container logs |
| `make down` | stop containers, keep data |
| `make reset` | stop and **delete all data** (re-runs the Postgres init script on the next `up`) |
| `make build` | `./mvnw package -DskipTests` |
| `make test` | `./mvnw verify` |

### `docs/adr/0001–0004`
The decisions made in this phase, with the alternatives considered: Maven multi-module, Spring Boot 4.1 + Java 21, Kafka in KRaft mode, and one Postgres container with a database per service.

---

## 7. Your turn: run and verify Phase 0

Run these in the Codespace terminal, from the repo root:

```bash
# 1. Rebuild the Codespace so the devcontainer applies (once):
#    Ctrl/Cmd+Shift+P → "Codespaces: Rebuild Container"

# 2. Check the toolchain
java -version        # 21
mvn -v               # 3.9.x
docker --version
node -v              # 22

# 3. Generate the Maven wrapper (once), then commit it
mvn -N wrapper:wrapper -Dmaven=3.9.11

# 4. Build everything and run the smoke tests
./mvnw verify

# 5. Start the infrastructure, then wait until everything is healthy
make up
make ps

# 6. Start one service and check its health
./mvnw -pl services/event-service spring-boot:run
#   in a second terminal:
curl localhost:8081/actuator/health
```

Then open these in the **Ports** tab:

- **Keycloak** (8180): log in as `admin` / `admin`
- **Mailpit** (8025): an empty inbox
- **Kafka UI** (8090): a cluster named `local`, with no topics yet

### Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `port is already allocated` | Something else uses that host port | `docker ps` to find it, or change the host side of the mapping |
| Postgres databases missing | The volume existed before the init script was added | `make reset && make up` |
| Kafka unhealthy for ~30 s | Normal first start | Wait, then check `docker logs et-kafka` |
| Keycloak page loops or fails through the Codespaces URL | Dev mode and proxy hostnames | Fine for Phase 0. Hostname settings come in Phase 2. |
| `./mvnw: Permission denied` | The wrapper isn't executable | `chmod +x mvnw` |
| Service won't start: `Port 8081 was already in use` | An older run is still alive | Stop it with `Ctrl+C`, or `lsof -i :8081` |

When everything works, tick the Phase 0 boxes in the root README and commit. Next is **Phase 1: event-service CRUD**.

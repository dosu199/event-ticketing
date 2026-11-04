#!/usr/bin/env bash
# =====================================================================
# scripts/dev.sh: start the whole local stack with one command.
#
#   ./scripts/dev.sh          start infra + services + frontend
#   ./scripts/dev.sh --down   same, and also stop the containers on exit
#
# Ctrl+C stops the services and the frontend. Containers keep running
# (fast next start) unless --down is given. Logs: .logs/<name>.log
# =====================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

LOG_DIR="$ROOT/.logs"
STOP_CONTAINERS=false
[[ "${1:-}" == "--down" ]] && STOP_CONTAINERS=true

# name | port | URL to poll until ready | command
SERVICES=(
  "event-service|8081|http://localhost:8081/actuator/health|./mvnw -q -pl services/event-service spring-boot:run"
  "api-gateway|8080|http://localhost:8080/actuator/health|./mvnw -q -pl services/api-gateway spring-boot:run"
  "frontend|5173|http://localhost:5173|npm --prefix frontend run dev"
)

COLORS=(36 35 33 32 34)
PIDS=()

log()  { printf '\033[1m[dev]\033[0m %s\n' "$*"; }
fail() { printf '\033[31m[dev] %s\033[0m\n' "$*" >&2; exit 1; }

cleanup() {
  local code=$?
  trap - INT TERM EXIT
  echo
  log "Stopping services..."
  for pid in "${PIDS[@]}"; do
    kill -- "-$pid" 2>/dev/null || true          # whole process group (mvnw -> java, npm -> vite)
  done
  wait 2>/dev/null || true
  if $STOP_CONTAINERS; then
    log "Stopping containers..."
    make -s down
  else
    log "Containers are still running (make down to stop them)."
  fi
  exit "$code"
}
trap cleanup INT TERM EXIT

# curl exit code 7 = connection refused, i.e. nothing listens on the port (IPv4 or IPv6).
port_in_use() {
  local rc=0
  curl -s -o /dev/null --max-time 2 "http://localhost:$1" || rc=$?
  [[ $rc -ne 7 ]]
}

wait_for() {
  local url=$1 timeout=${2:-180} waited=0
  while true; do
    if [[ "$url" == */actuator/health ]]; then
      curl -fs --max-time 2 "$url" 2>/dev/null | grep -Eq '"status" *: *"UP"' && return 0
    else
      curl -fs -o /dev/null --max-time 2 "$url" && return 0
    fi
    (( waited >= timeout )) && return 1
    sleep 2; waited=$((waited + 2))
  done
}

# ---- 1. Pre-flight checks -------------------------------------------
command -v docker >/dev/null || fail "docker not found"
[[ -x ./mvnw ]] || fail "./mvnw not found. Run: mvn -N wrapper:wrapper -Dmaven=3.9.11"
for svc in "${SERVICES[@]}"; do
  IFS='|' read -r name port _ _ <<<"$svc"
  port_in_use "$port" && fail "Port $port ($name) is already in use. Stop the old process first (lsof -i :$port)."
done
mkdir -p "$LOG_DIR"

# ---- 2. Infrastructure ----------------------------------------------
log "Starting infrastructure (docker compose)..."
make -s up >/dev/null
log "Waiting for Postgres to be healthy..."
for _ in $(seq 1 60); do
  [[ "$(docker inspect -f '{{.State.Health.Status}}' et-postgres 2>/dev/null)" == "healthy" ]] && break
  sleep 2
done
[[ "$(docker inspect -f '{{.State.Health.Status}}' et-postgres 2>/dev/null)" == "healthy" ]] \
  || fail "Postgres did not become healthy. Check: docker logs et-postgres"

# ---- 3. Frontend dependencies ---------------------------------------
if [[ ! -d frontend/node_modules ]]; then
  log "Installing frontend dependencies (first run)..."
  npm --prefix frontend install --silent
fi

# ---- 4. Services ------------------------------------------------------
i=0
for svc in "${SERVICES[@]}"; do
  IFS='|' read -r name port _ cmd <<<"$svc"
  color=${COLORS[$((i % ${#COLORS[@]}))]}
  : > "$LOG_DIR/$name.log"
  # setsid: own process group, so Ctrl+C cleanup can stop mvnw/npm AND their children.
  # Output goes to .logs/<name>.log and to this terminal with a colored [name] prefix.
  setsid bash -c "$cmd 2>&1 | tee -a '$LOG_DIR/$name.log' | sed -u 's/^/\x1b[${color}m[$name]\x1b[0m /'" &
  PIDS+=($!)
  log "Started $name (port $port), log: .logs/$name.log"
  i=$((i + 1))
done

# ---- 5. Wait until everything answers --------------------------------
for svc in "${SERVICES[@]}"; do
  IFS='|' read -r name _ url _ <<<"$svc"
  if wait_for "$url"; then
    log "$name is up"
  else
    fail "$name did not start within 3 minutes. See .logs/$name.log"
  fi
done

cat <<EOF

$(printf '\033[1;32m')All up.$(printf '\033[0m') Press Ctrl+C to stop.

  Frontend          http://localhost:5173
  API (gateway)     http://localhost:8080/api/events
  event-service     http://localhost:8081/actuator/health
  Keycloak          http://localhost:8180   (admin / admin)
  Mailpit           http://localhost:8025
  Kafka UI          http://localhost:8090

EOF

wait
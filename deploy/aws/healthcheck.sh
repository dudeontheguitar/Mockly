#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ENV_FILE:-$SCRIPT_DIR/.env.aws}"
COMPOSE_FILE="${COMPOSE_FILE:-$SCRIPT_DIR/docker-compose.yml}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing env file: $ENV_FILE"
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

echo "[health] API"
curl -fsS "https://${API_DOMAIN}/actuator/health" | cat
echo

echo "[health] AI"
curl -fsS "https://${ML_DOMAIN}/ping" | cat
echo

echo "[health] compose"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps

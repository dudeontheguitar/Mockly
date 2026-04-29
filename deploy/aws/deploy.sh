#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ENV_FILE:-$SCRIPT_DIR/.env.aws}"
COMPOSE_FILE="${COMPOSE_FILE:-$SCRIPT_DIR/docker-compose.yml}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing env file: $ENV_FILE"
  echo "Copy deploy/aws/.env.aws.example to deploy/aws/.env.aws and fill real values."
  exit 1
fi

PROFILE_ARGS=()
OLLAMA_ENABLED_VALUE="$(grep -E '^OLLAMA_ENABLED=' "$ENV_FILE" | tail -n 1 | cut -d'=' -f2- | tr -d '"' || true)"
OLLAMA_ENABLED_VALUE="$(echo "$OLLAMA_ENABLED_VALUE" | tr '[:upper:]' '[:lower:]')"
OLLAMA_MODEL_VALUE="$(grep -E '^OLLAMA_MODEL=' "$ENV_FILE" | tail -n 1 | cut -d'=' -f2- | tr -d '"' || true)"
if [[ -z "$OLLAMA_MODEL_VALUE" ]]; then
  OLLAMA_MODEL_VALUE="qwen2.5:0.5b"
fi
if [[ "$OLLAMA_ENABLED_VALUE" == "1" || "$OLLAMA_ENABLED_VALUE" == "true" || "$OLLAMA_ENABLED_VALUE" == "yes" ]]; then
  PROFILE_ARGS=(--profile ollama)
fi

echo "[deploy] compose file: $COMPOSE_FILE"
echo "[deploy] env file: $ENV_FILE"
if [[ ${#PROFILE_ARGS[@]} -gt 0 ]]; then
  echo "[deploy] enabling profile: ollama"
fi

docker compose "${PROFILE_ARGS[@]}" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" pull --ignore-pull-failures || true
docker compose "${PROFILE_ARGS[@]}" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build --remove-orphans
if [[ ${#PROFILE_ARGS[@]} -gt 0 ]]; then
  echo "[deploy] pulling ollama model: $OLLAMA_MODEL_VALUE"
  docker compose "${PROFILE_ARGS[@]}" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T ollama ollama pull "$OLLAMA_MODEL_VALUE"
fi
docker compose "${PROFILE_ARGS[@]}" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps

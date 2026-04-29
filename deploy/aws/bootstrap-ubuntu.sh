#!/usr/bin/env bash
set -euo pipefail

echo "[bootstrap] Updating packages"
sudo apt-get update -y
sudo apt-get install -y ca-certificates curl git

if ! command -v docker >/dev/null 2>&1; then
  echo "[bootstrap] Installing Docker"
  curl -fsSL https://get.docker.com | sudo sh
else
  echo "[bootstrap] Docker is already installed"
fi

sudo systemctl enable docker
sudo systemctl start docker

if id -nG "$USER" | grep -qw docker; then
  echo "[bootstrap] User $USER is already in docker group"
else
  echo "[bootstrap] Adding $USER to docker group"
  sudo usermod -aG docker "$USER"
  echo "[bootstrap] Re-login is required to apply docker group membership."
fi

docker --version
docker compose version

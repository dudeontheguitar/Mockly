# AWS Deployment (EC2 + Docker Compose + Domain + HTTPS)

Этот набор файлов поднимает ваш текущий стек на одном EC2:

- `app` (Spring backend)
- `ai` (FastAPI ML service)
- `postgres`
- `redis`
- `minio`
- `livekit` + `egress`
- `caddy` (TLS + reverse proxy на ваши домены)

## 1) Что важно заранее

- Нужен домен и доступ к DNS.
- Нужен EC2 Linux (рекомендуется Ubuntu 22.04/24.04).
- Для старта без GPU:
  - по умолчанию включен `OLLAMA_ENABLED=true` с легкой моделью `qwen2.5:0.5b`.
- Если EC2 не тянет Ollama:
  - поставьте `OLLAMA_ENABLED=false` (AI будет работать с fallback feedback без Ollama).

## 2) Минимальная схема DNS

Создайте 3 A-записи на публичный IP EC2:

- `api.your-domain.com` -> `EC2_PUBLIC_IP`
- `ml.your-domain.com` -> `EC2_PUBLIC_IP`
- `rtc.your-domain.com` -> `EC2_PUBLIC_IP`

## 3) Security Group (Inbound)

Откройте порты:

- `22/tcp` (только ваш IP)
- `80/tcp` (0.0.0.0/0) для Let's Encrypt
- `443/tcp` (0.0.0.0/0)
- `7881/tcp` (0.0.0.0/0) LiveKit TCP fallback
- `50000-50100/udp` (0.0.0.0/0) LiveKit media

## 4) Подготовка сервера

На EC2:

```bash
git clone <YOUR_REPO_URL> /opt/mockly
cd /opt/mockly
chmod +x deploy/aws/*.sh
bash deploy/aws/bootstrap-ubuntu.sh
```

Пере-зайдите по SSH (или `newgrp docker`), чтобы заработала группа `docker`.

## 5) Заполнение env и запуск

```bash
cd /opt/mockly
cp deploy/aws/.env.aws.example deploy/aws/.env.aws
nano deploy/aws/.env.aws
```

Заполните реальные значения секретов и доменов, затем:

```bash
bash deploy/aws/deploy.sh
```

Проверка:

```bash
bash deploy/aws/healthcheck.sh
```

## 6) Ollama

По умолчанию в `deploy/aws/.env.aws.example` включена легкая модель:

- `OLLAMA_ENABLED=true`
- `OLLAMA_MODEL=qwen2.5:0.5b`

`deploy/aws/deploy.sh` сам включит compose profile `ollama` и выполнит `ollama pull` для выбранной модели.

Если нужно поменять модель, измените `OLLAMA_MODEL` в `deploy/aws/.env.aws` и запустите:

```bash
bash deploy/aws/deploy.sh
```

## 7) CI/CD через GitHub Actions

В репозитории есть workflow: `.github/workflows/deploy-aws-ec2.yml`.

Добавьте GitHub Secrets:

- `AWS_EC2_HOST`
- `AWS_EC2_USER`
- `AWS_EC2_SSH_KEY`

И Variables:

- `EC2_APP_DIR` (например `/opt/mockly`)
- `DEPLOY_REF` (например `main`)

Если SSH порт не `22`, поменяйте `port` в `.github/workflows/deploy-aws-ec2.yml`.

После пуша в `main` workflow:

1. Подключается к EC2 по SSH.
2. Делает `git fetch` + `git checkout` + `git pull`.
3. Запускает `deploy/aws/deploy.sh`.

## 8) Полезные команды

Логи:

```bash
docker compose --env-file deploy/aws/.env.aws -f deploy/aws/docker-compose.yml logs -f --tail=200
```

Перезапуск:

```bash
docker compose --env-file deploy/aws/.env.aws -f deploy/aws/docker-compose.yml up -d --build
```

Остановка:

```bash
docker compose --env-file deploy/aws/.env.aws -f deploy/aws/docker-compose.yml down
```

## 9) Безопасность

- Не коммитьте `deploy/aws/.env.aws`.
- Обязательно замените все дефолтные секреты.
- `backend/.env` уже содержит dev-секреты; перед продом их нужно ротировать.

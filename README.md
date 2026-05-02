# Mockly

Mockly is a multi-module mock interview platform that combines a Spring Boot backend and an Android client to support interview practice, live communication, and AI-assisted feedback.

## Problem Statement

Preparing for technical and behavioral interviews is often inconsistent and difficult to practice in a realistic environment. Mockly aims to make interview preparation more structured by providing a platform where users can simulate interviews, communicate in real time, and receive generated feedback and reports.

## Project Structure

This repository contains two main parts:

- `backend/` - Spring Boot backend application
- `ai/` - Python FastAPI AI/ML evaluation service
- `android/` - Android mobile client

## Features

- User registration and authentication
- Public interview slot creation, booking, and cancellation
- Mock interview session lifecycle management
- LiveKit room and token support for real-time interviews
- Direct conversations and chat messages
- User profiles, profile settings, security settings, and preferences
- Avatar and interview artifact upload through MinIO
- Interview analysis integration with ML services
- Report generation after interview sessions
- User notifications for interviews and messages
- Mobile client for user-side interaction

## Backend Services

- `AuthService` - registration, login, refresh tokens, logout, and password changes.
- `UserService` - current user data, profile updates, account deletion, and interviewer discovery.
- `SettingsService` - language, theme, notification preferences, and timezone settings.
- `InterviewSlotService` - interviewer-created public slots, candidate booking, cancellation, and session creation from booked slots.
- `SessionService` - interview session creation, joining, leaving, ending, LiveKit room lifecycle, and participant management.
- `LiveKitService` - LiveKit room creation, room cleanup, access token generation, and recording support.
- `ArtifactService` - interview audio artifact upload flow and metadata storage.
- `FileService` - avatar upload request and completion flow.
- `ReportService` - report lookup and manual report generation trigger.
- `ReportProcessingService` - asynchronous ML report processing after audio upload or session end.
- `ChatService` - direct conversations, messages, read state, unread counts, and message deletion.
- `NotificationService` - notification listing, unread counts, mark-as-read, and mark-all-read.
- `MinIOService` - pre-signed upload/download URLs and object metadata access.
- `MLServiceClient` - communication with the external ML service.

## AI Service

The `ai/` module is a separate FastAPI service used by the backend for interview evaluation.

- `main.py` exposes the AI API.
- `GET /ping` checks service health.
- `POST /api/evaluate` receives interview data and returns evaluation results.
- `services/evaluator.py` contains the evaluation workflow.
- `services/scoring.py` calculates report scores and metrics.
- `services/audio_service.py` and `speech_to_text.py` handle audio and speech-to-text processing.
- `services/llm_client.py` connects the service to the LLM layer.
- `schemas.py` defines request and response models.

## Technology Stack

### Backend

- Java 21
- Spring Boot
- PostgreSQL
- Redis
- MinIO
- Flyway
- JWT Authentication
- WebSocket / STOMP
- LiveKit
- Docker / Docker Compose
- Maven

### AI Service

- Python 3.11
- FastAPI
- Uvicorn
- Faster Whisper
- Sentence Transformers
- PyTorch
- Ollama / LLM integration

### Mobile

- Kotlin
- Android SDK
- Gradle Kotlin DSL

## Installation

### 1. Clone the repository

```bash
git clone <your-repository-url>
cd Mockly
```

### 2. Backend setup

```bash
cd backend
mvn clean install
```

If Docker infrastructure is configured for local development, run:

```bash
docker compose up -d
```

Then start the backend application.

### 3. Android setup

Open the `android/` folder in Android Studio and let Gradle sync the project.

## Usage

- Run the backend service
- Start the Android application on an emulator or physical device
- Register or sign in
- Create or join a mock interview session
- Complete the session and review generated results or reports

## Notes

- The backend module has its own internal README with more detailed setup instructions
- AWS production deployment guide (EC2 + domain + HTTPS + CI/CD) is available at `deploy/aws/README.md`

## Student IDs

- 230103229
- 230103133
- 230103006
- 230103295

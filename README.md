# Mockly

Mockly is a multi-module mock interview platform that combines a Spring Boot backend and an Android client to support interview practice, live communication, and AI-assisted feedback.

## Problem Statement
Preparing for technical and behavioral interviews is often inconsistent and difficult to practice in a realistic environment. Mockly aims to make interview preparation more structured by providing a platform where users can simulate interviews, communicate in real time, and receive generated feedback and reports.

## Project Structure
This repository contains two main parts:

- `backend/` — Spring Boot backend application
- `android/` — Android mobile client

## Features
- User registration and authentication
- Mock interview session management
- Real-time communication support
- Audio or media artifact handling
- Interview analysis integration with ML services
- Report generation after interview sessions
- Mobile client for user-side interaction

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
- Docker / Docker Compose
- Maven

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


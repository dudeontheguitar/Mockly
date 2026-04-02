# Mockly Backend

Backend platform for conducting mock interviews with WebRTC support, audio processing through ML services, and report generation.

## Table of Contents

- [Description](#description)
- [Technologies](#technologies)
- [Requirements](#requirements)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Project](#running-the-project)
- [Testing](#testing)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)

## Description

Mockly is a platform for conducting mock interviews with the following features:

- Authentication and authorization - JWT tokens, registration, login
- Session management - creating, joining, and ending interviews
- WebRTC integration - LiveKit for video calls
- Artifact upload - MinIO for storing audio files (up to 500MB)
- ML processing - integration with an ML service for interview analysis
- Report generation - automatic generation of reports with metrics
- WebSocket - real-time updates via STOMP
- Transcripts - storing interview transcripts

## Technologies

- Java 21 - programming language
- Spring Boot 3.3.2 - framework
- PostgreSQL 16 - database
- Redis 7 - caching
- MinIO - S3-compatible storage
- Flyway - database migrations
- JWT - authentication
- WebSocket/STOMP - real-time communication
- LiveKit - WebRTC platform
- Maven - dependency management

## Requirements

Before starting, make sure the following are installed:

- Java 21 or higher
- Maven 3.8+
- Docker and Docker Compose (for infrastructure)
- Git

Version check:

```bash
java -version  # Must be Java 21+
mvn -version   # Must be Maven 3.8+
docker --version
docker-compose --version
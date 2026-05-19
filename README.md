# VoxOps Command Center

VoxOps Command Center is a production-style Voice AI incident command platform for SRE, DevOps, and platform engineering teams.

It lets responders create incidents, capture timeline events, stream browser microphone audio to a Java WebSocket service, generate transcript activity, produce AI recommendations from incident context, publish Kafka domain events, persist an audit trail, and monitor the system through Prometheus/Grafana.

This project is designed to demonstrate backend engineering, applied AI, event-driven architecture, observability, and product thinking.

## Core Capabilities

- Incident creation and incident detail pages.
- PostgreSQL persistence with Flyway migrations.
- Timeline events for manual and system-generated incident activity.
- Browser microphone capture with WebSocket audio streaming.
- Simulated transcript generation from live voice activity.
- AI recommendations using incident timeline and transcript context.
- AI provider abstraction for `MOCK`, `OPENAI`, `ANTHROPIC`, and `OLLAMA`.
- Kafka domain event publishing for incidents, timelines, transcripts, and recommendations.
- Persistent event audit log with Kafka topic, partition, offset, and publish status.
- Custom Micrometer metrics exposed through Spring Actuator.
- Provisioned Grafana dashboard for VoxOps metrics.

## Architecture Summary

```text
Next.js UI
  |
  | HTTP
  v
incident-service (Java 21, Spring Boot)
  |-- PostgreSQL: incidents, timeline, transcripts, recommendations, event audit log
  |-- Kafka producer: domain events
  |-- Micrometer/Actuator: Prometheus metrics
  |-- AI provider abstraction: Mock/OpenAI/Anthropic/Ollama

Browser microphone
  |
  | WebSocket audio chunks
  v
voice-stream-service (Java 21, Spring Boot WebSocket)
  |-- Sends simulated transcript events to incident-service
  |-- Emits voice streaming metrics

Prometheus
  |-- Scrapes incident-service and voice-stream-service

Grafana
  |-- Provisioned VoxOps Command Center dashboard
```

## Tech Stack

- Backend: Java 21, Spring Boot, Spring Web, Spring Data JPA, Spring Kafka, Maven
- Frontend: Next.js, React, TypeScript
- Data: PostgreSQL, Redis
- Messaging: Apache Kafka
- AI: OpenAI integration, Ollama integration, provider abstraction
- Voice: Browser MediaRecorder, WebSocket streaming
- Observability: Micrometer, Prometheus, Grafana
- DevOps: Docker Compose

## Prerequisites

- Java 21
- Maven
- Node.js LTS
- Docker Desktop
- Git
- Optional: OpenAI API key
- Optional: Ollama

## Start Local Infrastructure

From the project root:

```powershell
cd C:\Users\vaish\voxops-command-center
docker compose up -d
```

This starts:

- PostgreSQL on `55432`
- Redis on `6379`
- Kafka on `9092`
- Prometheus on `9090`
- Grafana on `3001`
- Qdrant on `6333`

## Build Backend Once

```powershell
cd C:\Users\vaish\voxops-command-center\backend
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& "C:\Program Files\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd" clean install
```

## Run Services

Open separate terminals.

### Terminal 1: incident-service

Use `MOCK` for local demo without API keys:

```powershell
cd C:\Users\vaish\voxops-command-center\backend

$env:SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:55432/voxops"
$env:SPRING_DATASOURCE_USERNAME="voxops"
$env:SPRING_DATASOURCE_PASSWORD="voxops"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
$env:VOXOPS_EVENTS_ENABLED="true"
$env:VOXOPS_AI_PROVIDER="MOCK"
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:Path="$env:JAVA_HOME\bin;$env:Path"

& "C:\Program Files\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd" spring-boot:run -pl services/incident-service
```

Use OpenAI instead:

```powershell
$env:VOXOPS_AI_PROVIDER="OPENAI"
$env:OPENAI_API_KEY="your-openai-api-key"
$env:OPENAI_MODEL="gpt-4o-mini"
```

Use Ollama instead:

```powershell
ollama pull llama3.2:1b
$env:VOXOPS_AI_PROVIDER="OLLAMA"
$env:OLLAMA_BASE_URL="http://localhost:11434"
$env:OLLAMA_MODEL="llama3.2:1b"
```

### Terminal 2: voice-stream-service

```powershell
cd C:\Users\vaish\voxops-command-center\backend

$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:Path="$env:JAVA_HOME\bin;$env:Path"

& "C:\Program Files\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd" spring-boot:run -pl services/voice-stream-service
```

### Terminal 3: frontend

```powershell
cd C:\Users\vaish\voxops-command-center\frontend
npm install
npm run dev
```

Open:

```text
http://localhost:3000
```

## Demo Script

Use this flow for a portfolio demo or screen recording.

1. Open the app at `http://localhost:3000`.
2. Sign in with one of the demo users:

```text
admin@voxops.dev / admin123
responder@voxops.dev / responder123
viewer@voxops.dev / viewer123
```

3. Create a new incident from the homepage.
4. Click the incident card to open the incident room.
5. Add a timeline note, such as:

```text
Investigating checkout latency after deployment.
```

6. Click **Start microphone** in the Voice Room panel.
7. Speak for a few seconds, then click **Stop**.
8. Refresh the incident detail page.
9. Confirm transcript segments appeared.
10. Click **Generate recommendation**.
11. Confirm the AI recommendation uses timeline and transcript context.
12. Scroll to **Domain Event Audit Log**.
13. Confirm events show topic, status, Kafka partition, and offset.
14. Open Grafana at `http://localhost:3001`.
15. Open:

```text
Dashboards -> VoxOps -> VoxOps Command Center
```

16. Show metrics for incidents, transcripts, voice chunks, AI recommendations, and Kafka events.

## Kafka Verification

Kafka events are published automatically when you create incidents, add timeline events, generate transcripts, and generate AI recommendations.

To inspect one topic directly:

```powershell
docker exec -it voxops-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic incident.created --from-beginning
```

Expected topics:

- `incident.created`
- `timeline.event.created`
- `transcript.segment.created`
- `ai.recommendation.created`

## Observability

Prometheus:

```text
http://localhost:9090
```

Service metrics:

```text
http://localhost:8081/actuator/prometheus
http://localhost:8082/actuator/prometheus
```

Search for:

```text
voxops_
```

Grafana:

```text
http://localhost:3001
admin / admin
```

Dashboard:

```text
Dashboards -> VoxOps -> VoxOps Command Center
```

Custom metrics include:

- `voxops_incidents_total`
- `voxops_transcript_segments_total`
- `voxops_ai_recommendations_total`
- `voxops_ai_recommendation_generation_seconds`
- `voxops_kafka_events_total`
- `voxops_kafka_publish_seconds`
- `voxops_voice_audio_chunks_total`
- `voxops_voice_audio_chunk_bytes`

## Useful Local URLs

- Frontend: http://localhost:3000
- API Gateway: http://localhost:8080
- Incident Service: http://localhost:8081
- Voice Service: http://localhost:8082
- RAG Service: http://localhost:8083
- Grafana: http://localhost:3001
- Prometheus: http://localhost:9090
- Qdrant: http://localhost:6333

## Portfolio Talking Points

- Built a Java 21 Spring Boot backend with PostgreSQL, Kafka, WebSocket streaming, and observability.
- Designed event-driven workflows with durable audit logging.
- Implemented AI provider abstraction with mock, OpenAI, and Ollama support.
- Built a voice-native incident workflow with browser audio capture and transcript generation.
- Added JWT authentication with demo users and protected incident APIs.
- Added Prometheus metrics and a Grafana dashboard for production-style monitoring.
- Used Flyway migrations for database schema evolution.
- Added integration tests for auth, incident APIs, and dashboard summary.
- Added GitHub Actions CI for backend tests and frontend builds.

## CI

On every push or pull request to `main`/`master`, GitHub Actions runs:

- `mvn -pl services/incident-service test` (auth, incidents, dashboard integration tests)
- `npm run build` in `frontend`

Workflow file: `.github/workflows/ci.yml`

## Next Build Milestones

1. Add Kubernetes/Terraform deployment.
2. Add action approval workflows for AI recommendations.
3. Add production-grade STT provider integration (Deepgram/Whisper).
4. Add load tests for voice streaming and incident APIs.

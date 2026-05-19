# Architecture Notes

## Initial Service Boundaries

- `api-gateway`: external entry point for browser and API clients.
- `incident-service`: owns incident lifecycle, status, severity, and participants.
- `voice-stream-service`: owns WebSocket audio sessions, VAD, STT, and TTS integration.
- `rag-service`: owns document ingestion, embeddings, vector search, and grounded AI answers.
- `common`: shared DTOs and utilities that are stable across services.

## Near-Term Event Topics

- `incident.created`
- `incident.updated`
- `voice.audio-chunk.received`
- `transcript.segment.created`
- `ai.recommendation.created`
- `timeline.event.created`

## First Production Principles

- Keep user-facing APIs behind the gateway.
- Keep each service independently runnable.
- Publish state changes to Kafka once persistence is added.
- Require citations for AI answers that use operational knowledge.
- Add human approval before any action that changes infrastructure.

package com.voxops.voice;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VoiceWebSocketHandler extends BinaryWebSocketHandler {

    private static final Logger LOGGER = Logger.getLogger(VoiceWebSocketHandler.class.getName());

    private final HttpClient httpClient;
    private final String incidentServiceBaseUrl;
    private final String internalServiceToken;
    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicInteger> chunkCountsBySession = new ConcurrentHashMap<>();

    public VoiceWebSocketHandler(String incidentServiceBaseUrl, String internalServiceToken, MeterRegistry meterRegistry) {
        this.incidentServiceBaseUrl = incidentServiceBaseUrl;
        this.internalServiceToken = internalServiceToken;
        this.meterRegistry = meterRegistry;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        chunkCountsBySession.put(session.getId(), new AtomicInteger());
        UUID incidentId = extractIncidentId(session);
        if (incidentId != null) {
            meterRegistry.counter("voxops_voice_sessions_opened_total", "incident_present", "true").increment();
            publishTimelineEvent(
                    incidentId,
                    "NOTE",
                    "Voice stream connected. Browser microphone audio is being sent to voice-stream-service.",
                    "voice-stream-service"
            );
        }
        session.sendMessage(new TextMessage("voice-session-opened"));
        LOGGER.info(() -> "Voice WebSocket opened: session=" + session.getId() + ", uri=" + session.getUri());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        AtomicInteger chunkCount = chunkCountsBySession.computeIfAbsent(session.getId(), ignored -> new AtomicInteger());
        int currentChunk = chunkCount.incrementAndGet();

        UUID incidentId = extractIncidentId(session);
        meterRegistry.counter(
                "voxops_voice_audio_chunks_total",
                "incident_present", String.valueOf(incidentId != null)
        ).increment();
        meterRegistry.summary(
                "voxops_voice_audio_chunk_bytes",
                "incident_present", String.valueOf(incidentId != null)
        ).record(message.getPayloadLength());
        LOGGER.info(() -> "Received audio chunk: session=" + session.getId()
                + ", chunk=" + currentChunk
                + ", bytes=" + message.getPayloadLength());

        session.sendMessage(new TextMessage("received-audio-bytes:" + message.getPayloadLength()
                + "; chunks=" + currentChunk));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID incidentId = extractIncidentId(session);
        meterRegistry.counter(
                "voxops_voice_sessions_closed_total",
                "incident_present", String.valueOf(incidentId != null),
                "close_code", String.valueOf(status.getCode())
        ).increment();
        chunkCountsBySession.remove(session.getId());
        LOGGER.info(() -> "Voice WebSocket closed: session=" + session.getId()
                + ", code=" + status.getCode()
                + ", reason=" + status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        UUID incidentId = extractIncidentId(session);
        meterRegistry.counter(
                "voxops_voice_transport_errors_total",
                "incident_present", String.valueOf(incidentId != null)
        ).increment();
        LOGGER.log(Level.WARNING, "Voice WebSocket transport error: session=" + session.getId(), exception);
        super.handleTransportError(session, exception);
    }

    private UUID extractIncidentId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            return null;
        }

        Map<String, String> queryParams = parseQuery(uri.getQuery());
        String incidentId = queryParams.get("incidentId");
        if (incidentId == null || incidentId.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(incidentId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                params.put(
                        URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                );
            }
        }
        return params;
    }

    private void publishTimelineEvent(UUID incidentId, String eventType, String summary, String source) {
        String json = """
                {
                  "eventType": "%s",
                  "summary": "%s",
                  "source": "%s"
                }
                """.formatted(eventType, escapeJson(summary), escapeJson(source));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("%s/api/incidents/%s/timeline".formatted(incidentServiceBaseUrl, incidentId)))
                .timeout(Duration.ofSeconds(3))
                .header("content-type", "application/json")
                .header("X-Internal-Service-Token", internalServiceToken)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .exceptionally(error -> null);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

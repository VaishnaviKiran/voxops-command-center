package com.voxops.voice;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class VoiceWebSocketConfig implements WebSocketConfigurer {

    private final String incidentServiceBaseUrl;
    private final String internalServiceToken;
    private final MeterRegistry meterRegistry;

    public VoiceWebSocketConfig(
            @Value("${incident.service.base-url:http://localhost:8081}") String incidentServiceBaseUrl,
            @Value("${incident.service.internal-token:local-dev-internal-token}") String internalServiceToken,
            MeterRegistry meterRegistry
    ) {
        this.incidentServiceBaseUrl = incidentServiceBaseUrl;
        this.internalServiceToken = internalServiceToken;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new VoiceWebSocketHandler(incidentServiceBaseUrl, internalServiceToken, meterRegistry), "/ws/voice")
                .setAllowedOrigins("*");
    }

    @Bean
    public ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(1024 * 1024);
        container.setMaxTextMessageBufferSize(64 * 1024);
        return container;
    }
}

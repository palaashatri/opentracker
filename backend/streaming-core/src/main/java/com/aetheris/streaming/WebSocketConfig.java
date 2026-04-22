package com.aetheris.streaming;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final TelemetryStreamHandler telemetryStreamHandler;

    public WebSocketConfig(TelemetryStreamHandler telemetryStreamHandler) {
        this.telemetryStreamHandler = telemetryStreamHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(telemetryStreamHandler, "/ws/telemetry")
                .setAllowedOrigins("*");
    }
}

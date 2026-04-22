package com.aetheris.streaming;

import com.aetheris.shared.SpatialEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@EnableScheduling
public class TelemetryStreamHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(TelemetryStreamHandler.class);
    private final SpatialIndexService spatialIndexService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Track viewport per session: SessionID -> Viewport
    private final ConcurrentMap<String, Viewport> sessionViewports = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public TelemetryStreamHandler(SpatialIndexService spatialIndexService) {
        this.spatialIndexService = spatialIndexService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        logger.info("New telemetry connection: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        sessionViewports.remove(session.getId());
        logger.info("Telemetry connection closed: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        // Client sends viewport: {"minLat": -90, "maxLat": 90, "minLon": -180, "maxLon": 180}
        JsonNode node = objectMapper.readTree(message.getPayload());
        Viewport viewport = new Viewport(
                node.get("minLat").asDouble(),
                node.get("maxLat").asDouble(),
                node.get("minLon").asDouble(),
                node.get("maxLon").asDouble()
        );
        sessionViewports.put(session.getId(), viewport);
    }

    @Scheduled(fixedRate = 100) // 10Hz updates
    public void broadcastTelemetry() {
        sessions.values().forEach(session -> {
            Thread.ofVirtual().start(() -> {
                try {
                    Viewport viewport = sessionViewports.get(session.getId());
                    Collection<SpatialEntity> inBounds;
                    if (viewport == null) {
                        inBounds = spatialIndexService.getAllEntities();
                    } else {
                        inBounds = spatialIndexService.getEntitiesInBounds(
                                viewport.minLat, viewport.maxLat, viewport.minLon, viewport.maxLon);
                    }

                    for (SpatialEntity entity : inBounds) {
                        byte[] protoBytes = entity.toProto().toByteArray();
                        session.sendMessage(new BinaryMessage(protoBytes));
                    }
                } catch (IOException e) {
                    logger.error("Error broadcasting to session {}", session.getId(), e);
                }
            });
        });
    }

    private record Viewport(double minLat, double maxLat, double minLon, double maxLon) {}
}

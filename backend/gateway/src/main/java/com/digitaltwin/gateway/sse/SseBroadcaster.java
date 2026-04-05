package com.digitaltwin.gateway.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(SseBroadcaster.class);

    private final CopyOnWriteArrayList<SseEmitter> flightEmitters = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<SseEmitter> shipEmitters = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<SseEmitter> satelliteEmitters = new CopyOnWriteArrayList<>();

    /**
     * Register a new SSE emitter for flight position updates.
     *
     * @return a long-lived SseEmitter (timeout=0 means never time out server-side)
     */
    public SseEmitter registerFlight() {
        SseEmitter emitter = new SseEmitter(0L);
        flightEmitters.add(emitter);
        emitter.onCompletion(() -> flightEmitters.remove(emitter));
        emitter.onTimeout(() -> flightEmitters.remove(emitter));
        emitter.onError(e -> flightEmitters.remove(emitter));
        log.debug("Flight SSE client registered. Total flight clients: {}", flightEmitters.size());
        return emitter;
    }

    /**
     * Register a new SSE emitter for ship position updates.
     *
     * @return a long-lived SseEmitter
     */
    public SseEmitter registerShip() {
        SseEmitter emitter = new SseEmitter(0L);
        shipEmitters.add(emitter);
        emitter.onCompletion(() -> shipEmitters.remove(emitter));
        emitter.onTimeout(() -> shipEmitters.remove(emitter));
        emitter.onError(e -> shipEmitters.remove(emitter));
        log.debug("Ship SSE client registered. Total ship clients: {}", shipEmitters.size());
        return emitter;
    }

    /**
     * Broadcast a flight event JSON string to all registered flight SSE clients.
     *
     * @param json the raw JSON payload to send
     */
    public void broadcastFlight(String json) {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : flightEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("flight")
                        .data(json)
                        .build());
            } catch (Exception e) {
                log.debug("Removing dead flight SSE emitter: {}", e.getMessage());
                dead.add(emitter);
            }
        }
        flightEmitters.removeAll(dead);
    }

    /**
     * Broadcast a ship event JSON string to all registered ship SSE clients.
     *
     * @param json the raw JSON payload to send
     */
    public void broadcastShip(String json) {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : shipEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("ship")
                        .data(json)
                        .build());
            } catch (Exception e) {
                log.debug("Removing dead ship SSE emitter: {}", e.getMessage());
                dead.add(emitter);
            }
        }
        shipEmitters.removeAll(dead);
    }

    /**
     * Register a new SSE emitter for satellite position updates.
     *
     * @return a long-lived SseEmitter
     */
    public SseEmitter registerSatellite() {
        SseEmitter emitter = new SseEmitter(0L);
        satelliteEmitters.add(emitter);
        emitter.onCompletion(() -> satelliteEmitters.remove(emitter));
        emitter.onTimeout(() -> satelliteEmitters.remove(emitter));
        emitter.onError(e -> satelliteEmitters.remove(emitter));
        log.debug("Satellite SSE client registered. Total satellite clients: {}", satelliteEmitters.size());
        return emitter;
    }

    /**
     * Broadcast a satellite event JSON string to all registered satellite SSE clients.
     *
     * @param json the raw JSON payload to send
     */
    public void broadcastSatellite(String json) {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : satelliteEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("satellite")
                        .data(json)
                        .build());
            } catch (Exception e) {
                log.debug("Removing dead satellite SSE emitter: {}", e.getMessage());
                dead.add(emitter);
            }
        }
        satelliteEmitters.removeAll(dead);
    }

    public int getFlightClientCount() {
        return flightEmitters.size();
    }

    public int getShipClientCount() {
        return shipEmitters.size();
    }

    public int getSatelliteClientCount() {
        return satelliteEmitters.size();
    }
}

package com.digitaltwin.gateway.proxy;

import com.digitaltwin.shared.dto.AircraftPositionDto;
import com.digitaltwin.shared.dto.SatellitePositionDto;
import com.digitaltwin.shared.dto.SceneQueryDto;
import com.digitaltwin.shared.dto.SceneResultDto;
import com.digitaltwin.shared.dto.TrackDto;
import com.digitaltwin.shared.dto.VesselPositionDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class GeospatialClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeospatialClient(@Qualifier("geospatialRestClient") RestClient restClient,
                            ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public List<AircraftPositionDto> getFlights(double minLat, double maxLat,
                                                 double minLon, double maxLon) {
        String json = restClient.get()
                .uri("/internal/flights?minLat={minLat}&maxLat={maxLat}&minLon={minLon}&maxLon={maxLon}",
                        minLat, maxLat, minLon, maxLon)
                .retrieve()
                .body(String.class);
        try {
            return objectMapper.readValue(json, new TypeReference<List<AircraftPositionDto>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize flights response", e);
        }
    }

    public TrackDto getFlightTrack(UUID id, Instant from, Instant to) {
        String json = restClient.get()
                .uri("/internal/flights/{id}/track?from={from}&to={to}",
                        id.toString(), from.toString(), to.toString())
                .retrieve()
                .body(String.class);
        try {
            return objectMapper.readValue(json, TrackDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize flight track response", e);
        }
    }

    public List<VesselPositionDto> getShips(double minLat, double maxLat,
                                             double minLon, double maxLon) {
        String json = restClient.get()
                .uri("/internal/ships?minLat={minLat}&maxLat={maxLat}&minLon={minLon}&maxLon={maxLon}",
                        minLat, maxLat, minLon, maxLon)
                .retrieve()
                .body(String.class);
        try {
            return objectMapper.readValue(json, new TypeReference<List<VesselPositionDto>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize ships response", e);
        }
    }

    public TrackDto getShipTrack(UUID id, Instant from, Instant to) {
        String json = restClient.get()
                .uri("/internal/ships/{id}/track?from={from}&to={to}",
                        id.toString(), from.toString(), to.toString())
                .retrieve()
                .body(String.class);
        try {
            return objectMapper.readValue(json, TrackDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize ship track response", e);
        }
    }

    public List<SatellitePositionDto> getSatellites() {
        String json = restClient.get()
                .uri("/internal/satellites")
                .retrieve()
                .body(String.class);
        try {
            return objectMapper.readValue(json, new TypeReference<List<SatellitePositionDto>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize satellites response", e);
        }
    }

    public SceneResultDto queryScene(SceneQueryDto query) {
        String body;
        try {
            body = objectMapper.writeValueAsString(query);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize scene query", e);
        }

        String json = restClient.post()
                .uri("/internal/scene/query")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            return objectMapper.readValue(json, SceneResultDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize scene result", e);
        }
    }
}

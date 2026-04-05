package com.digitaltwin.geospatial.service;

import com.digitaltwin.shared.dto.AircraftPositionDto;
import com.digitaltwin.shared.dto.SceneQueryDto;
import com.digitaltwin.shared.dto.SceneResultDto;
import com.digitaltwin.shared.dto.VesselPositionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SceneQueryService {

    private final FlightQueryService flightQueryService;
    private final ShipQueryService shipQueryService;

    public SceneResultDto query(SceneQueryDto request) {
        List<String> layers = request.layers() != null ? request.layers() : Collections.emptyList();

        List<AircraftPositionDto> flights = Collections.emptyList();
        List<VesselPositionDto> ships = Collections.emptyList();

        if (layers.isEmpty() || layers.contains("flights")) {
            try {
                flights = flightQueryService.getCurrentFlights(
                        request.minLat(), request.maxLat(),
                        request.minLon(), request.maxLon()
                );
            } catch (Exception e) {
                log.error("Failed to query flights for scene", e);
                flights = Collections.emptyList();
            }
        }

        if (layers.isEmpty() || layers.contains("ships")) {
            try {
                ships = shipQueryService.getCurrentShips(
                        request.minLat(), request.maxLat(),
                        request.minLon(), request.maxLon()
                );
            } catch (Exception e) {
                log.error("Failed to query ships for scene", e);
                ships = Collections.emptyList();
            }
        }

        return new SceneResultDto(flights, ships, Instant.now());
    }
}

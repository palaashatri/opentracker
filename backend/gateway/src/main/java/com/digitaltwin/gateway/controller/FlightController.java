package com.digitaltwin.gateway.controller;

import com.digitaltwin.gateway.proxy.GeospatialClient;
import com.digitaltwin.shared.dto.AircraftPositionDto;
import com.digitaltwin.shared.dto.TrackDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final GeospatialClient geospatialClient;

    public FlightController(GeospatialClient geospatialClient) {
        this.geospatialClient = geospatialClient;
    }

    /**
     * Returns current aircraft positions within the given bounding box.
     * Defaults to the entire globe if no parameters are supplied.
     */
    @GetMapping
    public List<AircraftPositionDto> getFlights(
            @RequestParam(defaultValue = "-90") double minLat,
            @RequestParam(defaultValue = "90") double maxLat,
            @RequestParam(defaultValue = "-180") double minLon,
            @RequestParam(defaultValue = "180") double maxLon) {
        return geospatialClient.getFlights(minLat, maxLat, minLon, maxLon);
    }

    /**
     * Returns the historical track for a specific aircraft between {@code from} and {@code to}.
     *
     * @param id   the aircraft entity UUID
     * @param from start of the time range (ISO-8601)
     * @param to   end of the time range (ISO-8601)
     */
    @GetMapping("/{id}/track")
    public TrackDto getTrack(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return geospatialClient.getFlightTrack(id, from, to);
    }
}

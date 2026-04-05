package com.digitaltwin.gateway.controller;

import com.digitaltwin.gateway.proxy.GeospatialClient;
import com.digitaltwin.shared.dto.TrackDto;
import com.digitaltwin.shared.dto.VesselPositionDto;
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
@RequestMapping("/api/ships")
public class ShipController {

    private final GeospatialClient geospatialClient;

    public ShipController(GeospatialClient geospatialClient) {
        this.geospatialClient = geospatialClient;
    }

    /**
     * Returns current vessel positions within the given bounding box.
     * Defaults to the entire globe if no parameters are supplied.
     */
    @GetMapping
    public List<VesselPositionDto> getShips(
            @RequestParam(defaultValue = "-90") double minLat,
            @RequestParam(defaultValue = "90") double maxLat,
            @RequestParam(defaultValue = "-180") double minLon,
            @RequestParam(defaultValue = "180") double maxLon) {
        return geospatialClient.getShips(minLat, maxLat, minLon, maxLon);
    }

    /**
     * Returns the historical track for a specific vessel between {@code from} and {@code to}.
     *
     * @param id   the vessel entity UUID
     * @param from start of the time range (ISO-8601)
     * @param to   end of the time range (ISO-8601)
     */
    @GetMapping("/{id}/track")
    public TrackDto getTrack(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return geospatialClient.getShipTrack(id, from, to);
    }
}

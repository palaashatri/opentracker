package com.digitaltwin.geospatial.controller;

import com.digitaltwin.geospatial.service.FlightQueryService;
import com.digitaltwin.shared.dto.AircraftPositionDto;
import com.digitaltwin.shared.dto.TrackDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/internal/flights")
@RequiredArgsConstructor
@Slf4j
public class FlightQueryController {

    private final FlightQueryService flightQueryService;

    @GetMapping
    public List<AircraftPositionDto> getFlights(
            @RequestParam double minLat,
            @RequestParam double maxLat,
            @RequestParam double minLon,
            @RequestParam double maxLon
    ) {
        log.debug("Query flights bbox minLat={} maxLat={} minLon={} maxLon={}",
                minLat, maxLat, minLon, maxLon);
        return flightQueryService.getCurrentFlights(minLat, maxLat, minLon, maxLon);
    }

    @GetMapping("/{id}/track")
    public TrackDto getTrack(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        log.debug("Query flight track id={} from={} to={}", id, from, to);
        return flightQueryService.getTrack(id, from, to);
    }
}

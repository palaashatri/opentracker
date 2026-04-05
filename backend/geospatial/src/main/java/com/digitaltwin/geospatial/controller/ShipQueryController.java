package com.digitaltwin.geospatial.controller;

import com.digitaltwin.geospatial.service.ShipQueryService;
import com.digitaltwin.shared.dto.TrackDto;
import com.digitaltwin.shared.dto.VesselPositionDto;
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
@RequestMapping("/internal/ships")
@RequiredArgsConstructor
@Slf4j
public class ShipQueryController {

    private final ShipQueryService shipQueryService;

    @GetMapping
    public List<VesselPositionDto> getShips(
            @RequestParam double minLat,
            @RequestParam double maxLat,
            @RequestParam double minLon,
            @RequestParam double maxLon
    ) {
        log.debug("Query ships bbox minLat={} maxLat={} minLon={} maxLon={}",
                minLat, maxLat, minLon, maxLon);
        return shipQueryService.getCurrentShips(minLat, maxLat, minLon, maxLon);
    }

    @GetMapping("/{id}/track")
    public TrackDto getTrack(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        log.debug("Query ship track id={} from={} to={}", id, from, to);
        return shipQueryService.getTrack(id, from, to);
    }
}

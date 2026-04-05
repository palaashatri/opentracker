package com.digitaltwin.geospatial.controller;

import com.digitaltwin.geospatial.service.SatelliteQueryService;
import com.digitaltwin.shared.dto.SatellitePositionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/satellites")
@RequiredArgsConstructor
public class SatelliteController {

    private final SatelliteQueryService satelliteQueryService;

    @GetMapping
    public List<SatellitePositionDto> getSatellites() {
        return satelliteQueryService.getAllSatellites();
    }
}

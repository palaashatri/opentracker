package com.digitaltwin.gateway.controller;

import com.digitaltwin.gateway.proxy.GeospatialClient;
import com.digitaltwin.shared.dto.SatellitePositionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/satellites")
@RequiredArgsConstructor
public class SatelliteController {

    private final GeospatialClient geospatialClient;

    @GetMapping
    public List<SatellitePositionDto> getSatellites() {
        return geospatialClient.getSatellites();
    }
}

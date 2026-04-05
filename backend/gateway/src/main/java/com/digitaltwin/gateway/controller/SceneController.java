package com.digitaltwin.gateway.controller;

import com.digitaltwin.gateway.proxy.GeospatialClient;
import com.digitaltwin.shared.dto.SceneQueryDto;
import com.digitaltwin.shared.dto.SceneResultDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scene")
public class SceneController {

    private final GeospatialClient geospatialClient;

    public SceneController(GeospatialClient geospatialClient) {
        this.geospatialClient = geospatialClient;
    }

    /**
     * Query a scene for all tracked entities within the given bounding box and time range.
     * Supports filtering by layer names (e.g. "flights", "ships").
     *
     * @param request bounding box, time range, and layer filters
     * @return aggregated result containing flights and ships
     */
    @PostMapping("/query")
    public SceneResultDto query(@RequestBody SceneQueryDto request) {
        return geospatialClient.queryScene(request);
    }
}

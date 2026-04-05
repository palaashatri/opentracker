package com.digitaltwin.geospatial.controller;

import com.digitaltwin.geospatial.service.SceneQueryService;
import com.digitaltwin.shared.dto.SceneQueryDto;
import com.digitaltwin.shared.dto.SceneResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/scene")
@RequiredArgsConstructor
@Slf4j
public class SceneController {

    private final SceneQueryService sceneQueryService;

    @PostMapping("/query")
    public SceneResultDto query(@RequestBody SceneQueryDto request) {
        log.debug("Scene query: bbox=[{},{},{},{}] layers={}",
                request.minLat(), request.maxLat(), request.minLon(), request.maxLon(),
                request.layers());
        return sceneQueryService.query(request);
    }
}

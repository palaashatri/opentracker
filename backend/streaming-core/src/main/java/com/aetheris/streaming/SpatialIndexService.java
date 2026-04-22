package com.aetheris.streaming;

import com.aetheris.shared.SpatialEntity;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class SpatialIndexService {
    // Simple spatial index: Map of entity ID to Entity
    // In production, this would be a QuadTree or H3-indexed structure
    private final ConcurrentMap<String, SpatialEntity> entities = new ConcurrentHashMap<>();

    public void updateEntity(SpatialEntity entity) {
        entities.put(entity.getId(), entity);
    }

    public void removeEntity(String id) {
        entities.remove(id);
    }

    public Collection<SpatialEntity> getEntitiesInBounds(double minLat, double maxLat, double minLon, double maxLon) {
        return entities.values().parallelStream()
                .filter(e -> isWithin(e, minLat, maxLat, minLon, maxLon))
                .collect(Collectors.toList());
    }

    private boolean isWithin(SpatialEntity e, double minLat, double maxLat, double minLon, double maxLon) {
        return e.getLatitude() >= minLat && e.getLatitude() <= maxLat &&
               e.getLongitude() >= minLon && e.getLongitude() <= maxLon;
    }
    
    public Collection<SpatialEntity> getAllEntities() {
        return entities.values();
    }
}

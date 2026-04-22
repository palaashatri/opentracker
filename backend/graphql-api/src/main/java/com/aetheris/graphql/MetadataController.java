package com.aetheris.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class MetadataController {

    @QueryMapping
    public EntityMetadata entityMetadata(@Argument String id) {
        // Mocking metadata lookup
        // In production, this would hit a DB (PostgreSQL/PostGIS)
        EntityMetadata meta = new EntityMetadata();
        meta.id = id;
        meta.name = "Mock Entity " + id;
        meta.country = "USA";
        return meta;
    }

    public static class EntityMetadata {
        public String id;
        public String name;
        public String country;
        public FlightMetadata flight;
        public ShipMetadata ship;
        public SatelliteMetadata satellite;
    }

    public static class FlightMetadata {
        public String airline;
        public String origin;
        public String destination;
        public String aircraftType;
    }

    public static class ShipMetadata {
        public String vesselName;
        public String vesselType;
        public String callsign;
    }

    public static class SatelliteMetadata {
        public String intlDesignator;
        public String launchDate;
        public String owner;
    }
}

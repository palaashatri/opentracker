rootProject.name = "digital-twin-earth"

include(
    "backend:shared",
    "backend:gateway",
    "backend:ingestion",
    "backend:stream-processor",
    "backend:geospatial"
)

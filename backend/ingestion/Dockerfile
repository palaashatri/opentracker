FROM eclipse-temurin:17 AS builder
WORKDIR /workspace

# Copy Gradle wrapper and root build files first (layer-cache friendly)
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

# Copy only the modules needed for this service
COPY backend/shared backend/shared
COPY backend/ingestion backend/ingestion

RUN test -f gradle/wrapper/gradle-wrapper.jar && chmod +x gradlew && \
    ./gradlew :backend:ingestion:bootJar --no-daemon -x test

# ── Runtime image ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17
WORKDIR /app

COPY --from=builder /workspace/backend/ingestion/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]

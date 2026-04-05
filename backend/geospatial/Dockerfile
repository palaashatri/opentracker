FROM eclipse-temurin:17 AS builder
WORKDIR /workspace

# Copy Gradle wrapper and root build files
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

# Copy shared module
COPY backend/shared backend/shared

# Copy geospatial module
COPY backend/geospatial backend/geospatial

RUN test -f gradle/wrapper/gradle-wrapper.jar && chmod +x gradlew && ./gradlew :backend:geospatial:bootJar --no-daemon -x test

FROM eclipse-temurin:17
WORKDIR /app

RUN groupadd -r appgroup && useradd --no-create-home -r -g appgroup appuser
USER appuser

COPY --from=builder /workspace/backend/geospatial/build/libs/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]

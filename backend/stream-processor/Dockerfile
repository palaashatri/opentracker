FROM eclipse-temurin:17 AS builder
WORKDIR /workspace

# Copy Gradle wrapper and root build files
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

# Copy shared module
COPY backend/shared backend/shared

# Copy stream-processor module
COPY backend/stream-processor backend/stream-processor

RUN test -f gradle/wrapper/gradle-wrapper.jar && chmod +x gradlew && ./gradlew :backend:stream-processor:bootJar --no-daemon -x test

FROM eclipse-temurin:17
WORKDIR /app

RUN groupadd -r appgroup && useradd --no-create-home -r -g appgroup appuser
USER appuser

COPY --from=builder /workspace/backend/stream-processor/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]

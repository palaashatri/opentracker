FROM eclipse-temurin:17 AS builder
WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY backend/shared backend/shared
COPY backend/gateway backend/gateway
RUN test -f gradle/wrapper/gradle-wrapper.jar && chmod +x gradlew && ./gradlew :backend:gateway:bootJar --no-daemon -x test

FROM eclipse-temurin:17
WORKDIR /app
COPY --from=builder /workspace/backend/gateway/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

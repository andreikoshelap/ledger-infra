# ---- build (uses the committed Gradle wrapper, so the Gradle version matches the repo) ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
# Warm the dependency layer first for faster rebuilds.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

# ---- run ----
FROM eclipse-temurin:21-jre AS run
WORKDIR /app
# curl only for the compose healthcheck against /actuator/health
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

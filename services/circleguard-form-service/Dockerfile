# Stage 1: Build
FROM gradle:8.6.0-jdk21 AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle :services:circleguard-form-service:bootJar --no-daemon -Dorg.gradle.jvmargs=-Xmx1536m

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/services/circleguard-form-service/build/libs/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
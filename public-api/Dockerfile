# Build stage: compile the service with its own Gradle wrapper.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
# A checkout on Windows can leave the wrapper with CRLF line endings, which Linux can't run.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
# Tests run outside Docker; the cache mount keeps Gradle and dependencies between builds.
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon -x test

# Run stage: only the JRE and the jar.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
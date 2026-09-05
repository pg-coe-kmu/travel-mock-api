# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -B -q dependency:resolve
COPY src ./src
RUN mvn -B -q package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/travel-mock-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
# The command-line argument forces the remote profile regardless of the
# build-time default, so this image always runs the remote application.
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=remote"]

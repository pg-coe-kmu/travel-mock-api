FROM maven:3.9-eclipse-temurin-23 AS build

WORKDIR /app

# Dependencies cachen (nur pom.xml kopieren)
COPY pom.xml .
RUN mvn dependency:go-offline

# Quellcode kopieren und bauen
COPY src ./src
COPY data ./data

RUN mvn clean package -DskipTests

FROM eclipse-temurin:23-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/data ./data

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
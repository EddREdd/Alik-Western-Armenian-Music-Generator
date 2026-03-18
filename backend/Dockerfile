FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .

# Pre-fetch dependencies/plugins so the final package step can run in offline mode.
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src

# Try offline packaging first; if any dependency is missing, fallback to online.
RUN mvn -q -DskipTests -o package || mvn -q -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/musicgen-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

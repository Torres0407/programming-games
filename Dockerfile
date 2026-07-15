# Stage 1: Build compilation environment
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package

# Stage 2: Clean, ultra-lightweight execution engine container
FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/target/*.jar game-app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "game-app.jar"]
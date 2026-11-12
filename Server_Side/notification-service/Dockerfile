# builder stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
# copy pom first for better caching
COPY pom.xml .
RUN mvn -B -ntp dependency:resolve

COPY src ./src
RUN mvn -B -DskipTests package

# runtime: JRE only. No port or healthcheck: the service has no HTTP server, it only listens to RabbitMQ.
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]

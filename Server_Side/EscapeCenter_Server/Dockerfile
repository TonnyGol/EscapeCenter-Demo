# builder stage
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app
# copy pom first for better caching
COPY pom.xml .
RUN mvn -B -ntp dependency:resolve

COPY src ./src
RUN mvn -B -DskipTests package

# runtime: JRE only, alpine so the healthcheck can use busybox wget
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
# Safe by default: the dev profile logs every SQL statement, customer data included.
ENV SPRING_PROFILES_ACTIVE=prod
# The container clock is UTC; game times are venue-local, so "has this game started" needs the venue zone.
ENV TZ=Asia/Jerusalem
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java","-jar","/app/app.jar"]

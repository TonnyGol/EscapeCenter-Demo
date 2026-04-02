# builder stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
# copy pom first for better caching
COPY pom.xml .
# If you have additional files (settings.xml) adjust
RUN mvn -B -ntp dependency:resolve

COPY src ./src
RUN mvn -B -DskipTests package

# runtime
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

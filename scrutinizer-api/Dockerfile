# Build context must be the repository root:
#   docker build -f scrutinizer-api/Dockerfile .
FROM eclipse-temurin:17-jdk AS build
WORKDIR /project

COPY gradlew ./
COPY gradle/ ./gradle/
COPY build.gradle settings.gradle ./
COPY scrutinizer-engine/ ./scrutinizer-engine/
COPY scrutinizer-api/ ./scrutinizer-api/

RUN chmod +x gradlew && ./gradlew :scrutinizer-api:bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /project/scrutinizer-api/build/libs/*.jar /app/scrutinizer-api.jar

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/scrutinizer
ENV SPRING_DATASOURCE_USERNAME=scrutinizer
ENV SPRING_DATASOURCE_PASSWORD=scrutinizer

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/scrutinizer-api.jar"]

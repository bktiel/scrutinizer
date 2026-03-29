FROM gradle:8-jdk17 AS build
WORKDIR /project
COPY build.gradle settings.gradle* ./
COPY src ./src
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /work
COPY --from=build /project/build/libs/scrutinizer.jar /app/scrutinizer.jar
ENTRYPOINT ["java", "-jar", "/app/scrutinizer.jar"]

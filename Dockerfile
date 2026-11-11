FROM gradle:9.8.2-jdk21-alpine AS builder

WORKDIR /workspace
COPY --chown=gradle:gradle build.gradle settings.gradle gradle.properties ./
RUN gradle dependencies --no-daemon

COPY --chown=gradle:gradle src src
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC -Dfile.encoding=UTF-8"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

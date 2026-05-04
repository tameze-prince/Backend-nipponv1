# syntax=docker/dockerfile:1.7

FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

RUN addgroup -g 1001 -S appuser \
    && adduser -u 1001 -S appuser -G appuser
USER appuser

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0 -XX:+UseStringDeduplication -XX:+ParallelRefProcEnabled -Dfile.encoding=UTF-8"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider "http://127.0.0.1:${PORT:-8080}/actuator/health" || exit 1

CMD ["sh", "-c", "java $JAVA_OPTS -Dspring.boot.server.port=${PORT:-8080} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -jar app.jar"]

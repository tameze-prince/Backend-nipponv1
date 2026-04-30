# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:resolve

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create optimized runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install dumb-init for proper signal handling
RUN apk add --no-cache dumb-init

# Copy the built JAR from builder
COPY --from=builder /build/target/*.jar app.jar

# Create non-root user for security
RUN addgroup -g 1001 -S appuser && adduser -u 1001 -S appuser -G appuser
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD java -cp app.jar org.springframework.boot.loader.JarLauncher org.springframework.boot.actuate.health.Health || exit 1

# Use dumb-init to handle signals properly
ENTRYPOINT ["/sbin/dumb-init", "--"]

# Start application with optimized JVM settings
CMD ["java", \
     "-XX:+UseG1GC", \
     "-XX:MaxRAMPercentage=75.0", \
     "-XX:InitialRAMPercentage=25.0", \
     "-XX:+UseStringDeduplication", \
     "-XX:+ParallelRefProcEnabled", \
     "-Dfile.encoding=UTF-8", \
     "-Dspring.profiles.active=prod", \
     "-jar", \
     "app.jar"]

# Dependencies stage - cache dependencies for faster rebuilds
FROM eclipse-temurin:21-jdk-alpine AS deps

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew* build.gradle* settings.gradle* ./
COPY gradle/ gradle/

# Make gradlew executable and ensure Unix line endings
RUN chmod +x gradlew && \
    sed -i 's/\r$//' gradlew

# Download dependencies first to cache layer
RUN ./gradlew dependencies --no-daemon

# Build stage
FROM deps AS builder

WORKDIR /app

# Copy source and build
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# JLink stage - create custom JRE with only necessary modules
FROM builder AS jlinker

WORKDIR /app

RUN jlink \
    --add-modules java.base,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,java.compiler,java.scripting,jdk.unsupported,java.security.sasl,java.xml.crypto,jdk.crypto.cryptoki \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /custom-jre

# Runtime stage - use Alpine with custom JRE (minimal size)
FROM alpine:3.20

# Install dumb-init for proper signal handling
RUN apk add --no-cache dumb-init

# Create non-root user for security
RUN addgroup -g 1001 -S appuser && \
    adduser -S appuser -u 1001 -G appuser

WORKDIR /app

# Copy custom JRE and application
COPY --from=jlinker /custom-jre /custom-jre
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8083

# Use dumb-init to handle signals properly with custom JRE
ENTRYPOINT ["/usr/bin/dumb-init", "--", "/custom-jre/bin/java", "-jar", "/app/app.jar"]

# Switch to non-root user for security
USER appuser
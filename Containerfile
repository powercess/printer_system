# Multi-stage build for printer_system
# Using Azul Zulu JDK 25
# Supports linux/amd64 and linux/arm64
#
# Stage 1: Build backend JAR
FROM docker.io/eclipse-temurin:21-jdk AS backend-builder

# Build argument for architecture
ARG TARGETARCH

# Download and install JDK 25 based on architecture
RUN apt-get update && apt-get install -y curl && \
    JDK_URL="https://cdn.azul.com/zulu/bin/zulu25.28.85-ca-jdk25.0.0-linux_x64.tar.gz" && \
    if [ "$TARGETARCH" = "arm64" ]; then \
        JDK_URL="https://cdn.azul.com/zulu/bin/zulu25.28.85-ca-jdk25.0.0-linux_aarch64.tar.gz"; \
    fi && \
    curl -fsSL "$JDK_URL" | tar xz -C /opt && \
    ln -s /opt/zulu25.28.85-ca-jdk25.0.0-linux_* /opt/jdk25 && \
    rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/opt/jdk25
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Verify Java installation
RUN java -version

WORKDIR /build

# Copy all files needed for build
COPY . .

# Build with Gradle using JDK 25
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Build frontend
FROM docker.io/oven/bun:1.2-debian AS frontend-builder

WORKDIR /build

# Copy package files first for better caching
COPY web/package.json web/bun.lock* ./

# Install dependencies
RUN bun install --frozen-lockfile

# Copy source and build
COPY web .
RUN bun run build

# Stage 3: Runtime image with JDK 25
FROM docker.io/debian:bookworm-slim

# Build argument for architecture
ARG TARGETARCH

# Install required packages
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js 22
RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash - && \
    apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*

# Download and install JDK 25 based on architecture
RUN JDK_URL="https://cdn.azul.com/zulu/bin/zulu25.28.85-ca-jdk25.0.0-linux_x64.tar.gz" && \
    if [ "$TARGETARCH" = "arm64" ]; then \
        JDK_URL="https://cdn.azul.com/zulu/bin/zulu25.28.85-ca-jdk25.0.0-linux_aarch64.tar.gz"; \
    fi && \
    curl -fsSL "$JDK_URL" | tar xz -C /opt && \
    ln -s /opt/zulu25.28.85-ca-jdk25.0.0-linux_* /opt/jdk25

ENV JAVA_HOME=/opt/jdk25
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Verify Java installation
RUN java -version

# Create app user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

# Copy backend JAR
COPY --from=backend-builder /build/build/libs/*.jar app.jar

# Copy frontend output
COPY --from=frontend-builder /build/.output .output

# Copy entrypoint script
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Create uploads directory
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose ports
# 8080 - Backend API (Spring Boot) - includes payment callback endpoints
# 3000 - Frontend (Nuxt)
EXPOSE 8080 3000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api-docs || exit 1

# Set environment variables
ENV NODE_ENV=production
ENV NUXT_HOST=0.0.0.0
ENV NUXT_PORT=3000

ENTRYPOINT ["/entrypoint.sh"]
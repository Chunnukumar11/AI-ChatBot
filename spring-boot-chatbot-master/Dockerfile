# Stage 1: Build stage
FROM maven:3.9.11-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -q

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/target/chatbot-1.0.0.jar app.jar

# Default port (Render injects PORT env var)
ENV PORT=8085
EXPOSE ${PORT}

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT}/ || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

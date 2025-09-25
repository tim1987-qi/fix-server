# Multi-stage build for FIX Server
FROM openjdk:8-jdk-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM openjdk:8-jre-alpine

# Add application user for security
RUN addgroup -g 1001 fixserver && \
    adduser -D -s /bin/sh -u 1001 -G fixserver fixserver

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/fix-server-*.jar app.jar

# Change ownership to application user
RUN chown -R fixserver:fixserver /app

# Switch to application user
USER fixserver

# Expose ports
EXPOSE 8080 9878 9879

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
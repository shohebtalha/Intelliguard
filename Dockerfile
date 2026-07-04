# ─── Stage 1: Build ───────────────────────────────────────
# Use Maven + Java 21 to compile and package the app
FROM maven:3.9.5-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml first and download dependencies
# This layer is cached — only re-runs if pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─── Stage 2: Run ─────────────────────────────────────────
# Use a lightweight JRE (not full JDK) for the final image
# This makes the image ~200MB instead of ~600MB
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy only the built JAR from stage 1
COPY --from=builder /app/target/*.jar app.jar



# Expose port 8080
EXPOSE 8080

# Health check — Docker will restart container if this fails
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
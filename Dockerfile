# ============================================
# Stage 1: Build
# ============================================
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy the single consolidated POM (cache dependencies) — see
# governance/project-artifacts/INTERFACE-VS-REST-AND-POM-STRUCTURE-RECOMMENDATION.md
COPY pom.xml ./pom.xml

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build (skip tests — run tests in CI, not in Docker build)
RUN mvn clean package -DskipTests -B

# ============================================
# Stage 2: Runtime
# ============================================
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# Security: run as non-root
RUN apk add --no-cache wget && \
	addgroup -S appgroup && \
	adduser -S appuser -G appgroup && \
	mkdir -p /app/logs && \
	chown -R appuser:appgroup /app

# Copy JAR from build stage
COPY --from=build /app/target/erp-system-*.jar app.jar

# Set ownership
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 7272

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]

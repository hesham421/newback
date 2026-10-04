# ============================================
# Build from the REPOSITORY ROOT (the build needs the root pom and erp-core):
#   docker build -f erp-app-reference/Dockerfile -t erp-app-reference .
# ============================================
# Stage 1: Build
# ============================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the POMs first (dependency layer cache): root aggregator + both modules
COPY pom.xml ./pom.xml
COPY erp-core/pom.xml ./erp-core/pom.xml
COPY erp-app-reference/pom.xml ./erp-app-reference/pom.xml

# Copy source code
COPY erp-core/src ./erp-core/src
COPY erp-app-reference/src ./erp-app-reference/src

# Build the reference app and the library it depends on (skip tests — run tests in CI, not in Docker build)
RUN mvn clean package -DskipTests -B -pl erp-app-reference -am

# ============================================
# Stage 2: Runtime
# ============================================
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Security: run as non-root
RUN apk add --no-cache wget && \
	addgroup -S appgroup && \
	adduser -S appuser -G appgroup && \
	mkdir -p /app/logs && \
	chown -R appuser:appgroup /app

# Copy JAR from build stage
COPY --from=build /app/erp-app-reference/target/erp-app-reference-*.jar app.jar

# Set ownership
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 7272

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]

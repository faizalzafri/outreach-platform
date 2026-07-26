# ───────────────────────────────────────────────────────────────
# Stage 1: Build — compile and package the JAR
# ───────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy all module POMs for dependency resolution (cached layer)
COPY pom.xml ./
COPY common-lib/pom.xml common-lib/pom.xml
COPY config-server/pom.xml config-server/pom.xml
COPY discovery-service/pom.xml discovery-service/pom.xml
COPY gateway-service/pom.xml gateway-service/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY event-service/pom.xml event-service/pom.xml
COPY feedback-service/pom.xml feedback-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ingestion-service/pom.xml ingestion-service/pom.xml
COPY report-service/pom.xml report-service/pom.xml
COPY ai-service/pom.xml ai-service/pom.xml

# Download dependencies (cached unless POMs change)
RUN mvn dependency:go-offline -B -pl config-server -am

# Copy source
COPY common-lib/src common-lib/src
COPY config-server/src config-server/src

# Build JAR
RUN mvn package -B -pl config-server -am -DskipTests

# ───────────────────────────────────────────────────────────────
# Stage 2: Runtime
# ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/config-server/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "app.jar"]

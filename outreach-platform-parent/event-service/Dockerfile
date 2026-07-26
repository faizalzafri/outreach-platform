# ───────────────────────────────────────────────────────────────
# Stage 1: Build
# ───────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

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

RUN mvn dependency:go-offline -B -pl event-service -am

COPY common-lib/src common-lib/src
COPY event-service/src event-service/src

RUN mvn package -B -pl event-service -am -DskipTests

# ───────────────────────────────────────────────────────────────
# Stage 2: Runtime
# ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/event-service/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 9004

ENTRYPOINT ["java", "-jar", "app.jar"]

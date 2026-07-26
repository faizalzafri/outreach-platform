# ───────────────────────────────────────────────────────────────
# Stage 1: Build
# ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY pom.xml mvnw ./
COPY .mvn .mvn

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

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -pl ingestion-service -am

COPY common-lib/src common-lib/src
COPY ingestion-service/src ingestion-service/src

RUN ./mvnw package -B -pl ingestion-service -am -DskipTests

# ───────────────────────────────────────────────────────────────
# Stage 2: Runtime
# ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/ingestion-service/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 9003

ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 9006

ENTRYPOINT ["java", "-jar", "app.jar"]

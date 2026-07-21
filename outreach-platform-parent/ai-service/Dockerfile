FROM eclipse-temurin:21-jre

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

COPY target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 9006

ENTRYPOINT ["java", "-jar", "app.jar"]

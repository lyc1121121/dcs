FROM eclipse-temurin:8-jre

WORKDIR /app
COPY target/dcsagent.jar /app/dcsagent.jar

ENTRYPOINT ["java", "-jar", "/app/dcsagent.jar"]

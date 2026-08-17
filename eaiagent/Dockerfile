FROM eclipse-temurin:8-jre

WORKDIR /app
COPY target/eaiagent.jar /app/eaiagent.jar

ENTRYPOINT ["java", "-jar", "/app/eaiagent.jar"]

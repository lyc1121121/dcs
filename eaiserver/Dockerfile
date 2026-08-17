FROM eclipse-temurin:8-jre

WORKDIR /app
COPY target/eaiserver.jar /app/eaiserver.jar

ENTRYPOINT ["java", "-jar", "/app/eaiserver.jar"]

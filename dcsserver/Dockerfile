FROM eclipse-temurin:8-jre

WORKDIR /app
COPY target/dcsserver.jar /app/dcsserver.jar

ENTRYPOINT ["java", "-jar", "/app/dcsserver.jar"]

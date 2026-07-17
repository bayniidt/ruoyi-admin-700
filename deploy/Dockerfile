FROM dockerproxy.net/library/eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY ruoyi-admin.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]

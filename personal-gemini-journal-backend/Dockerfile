FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/personal-gemini-journal-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]

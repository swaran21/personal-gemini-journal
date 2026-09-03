FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre
# Use a glibc-based runtime. Firestore's shaded gRPC TLS native library can
# segfault when loaded from an Alpine/musl image during Cloud Run startup.
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /workspace/target/personal-gemini-journal-backend-1.0.0.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-Djava.security.egd=file:/dev/urandom","-jar","/app/app.jar"]

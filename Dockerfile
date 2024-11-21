FROM eclipse-temurin:21-jdk as builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENV OUTPUT_DIR=/app/output
VOLUME /app/output
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
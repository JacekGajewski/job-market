# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew gradle.properties* ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Development stage
FROM eclipse-temurin:21-jdk-alpine AS development
WORKDIR /app
COPY gradlew gradle.properties* ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
COPY src src
EXPOSE 8080
EXPOSE 5005
CMD ["./gradlew", "bootRun", "--no-daemon"]

# Production stage
FROM eclipse-temurin:21-jre-alpine AS production
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
RUN apk add --no-cache curl
COPY --from=builder /app/build/libs/*.jar app.jar
RUN chown -R app:app /app
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

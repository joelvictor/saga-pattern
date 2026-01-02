# Build stage
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# Copy Gradle files
COPY ../gradlew .
COPY ../gradle gradle
COPY ../build.gradle.kts .
COPY ../settings.gradle.kts .
COPY ../gradle.properties .

# Copy shared kernel
COPY ../shared-kernel shared-kernel

# Copy this service
COPY . delivery-service

# Build
RUN chmod +x gradlew && ./gradlew :delivery-service:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:25-jre

WORKDIR /app

# Enable ZGC
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -Xmx512m"

# Copy jar
COPY --from=build /app/delivery-service/build/libs/delivery-service.jar app.jar

EXPOSE 8083

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

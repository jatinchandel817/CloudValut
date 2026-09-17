# ============================================================
#  Dockerfile – Distributed File Storage
#  Multi-stage build: Maven build → minimal JRE runtime
# ============================================================

# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml first to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create uploads directory
RUN mkdir -p /app/uploads

# Copy the built JAR
COPY --from=builder /app/target/*.jar /app/app.jar

# Expose the application port
EXPOSE 8080

# Environment variables (override at runtime)
ENV SPRING_DATASOURCE_URL=jdbc:mariadb://mariadb:3306/distributed_file_storage?createDatabaseIfNotExist=true
ENV SPRING_DATASOURCE_USERNAME=root
ENV SPRING_DATASOURCE_PASSWORD=changeme
ENV STORAGE_LOCAL_PATH=/app/uploads
ENV STORAGE_ENABLED_PROVIDERS=LOCAL

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

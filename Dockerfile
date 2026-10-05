# syntax=docker/dockerfile:1

# ---------- Build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Las dependencias primero: si no cambian, la capa se cachea
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Run ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# usuario sin privilegios
RUN groupadd -r spring && useradd -r -g spring spring
COPY --from=build /app/target/*.jar app.jar
RUN chown -R spring:spring /app
USER spring:spring

EXPOSE 8080

ENV JAVA_OPTS="" \
    DB_HOST=localhost \
    DB_PORT=3306 \
    DB_NAME=foodies \
    DB_USER=root \
    DB_PASS="" \
    ADMIN_PASSWORD=admin123 \
    JWT_SECRET=cambiar-esta-clave-por-una-de-32-bytes-o-mas

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
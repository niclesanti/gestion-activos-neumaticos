# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:24-jre-alpine AS final
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/target/*.jar app.jar
RUN chown spring:spring app.jar
# Logs ECS en archivo (los lee Alloy): el volumen nombrado de compose hereda
# este dueño, porque el contenedor corre con el sistema de archivos de solo lectura.
RUN mkdir /logs && chown spring:spring /logs
USER spring

# 8080: API. 8081: puerto de gestión (Actuator), solo red interna de Docker.
EXPOSE 8080 8081
# Memoria de la JVM (ver compose.yaml, mem_limit del backend):
# - MaxRAMPercentage: el heap es un porcentaje del límite del contenedor, no del
#   host (por defecto sería 25 %). El resto queda para metaspace, hilos y código.
# - SerialGC: menos memoria extra que G1 y sobra para una app de baja concurrencia.
# - ExitOnOutOfMemoryError: ante un OOM el proceso termina y el contenedor se
#   reinicia limpio, en vez de quedar colgado a medias.
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=60", \
  "-XX:+UseSerialGC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]

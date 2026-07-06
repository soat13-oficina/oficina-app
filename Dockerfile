FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S oficina && adduser -S oficina -G oficina
COPY --from=build /app/target/*.jar app.jar
RUN chown oficina:oficina app.jar
USER oficina:oficina

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:8080/v3/api-docs >/dev/null || exit 1

# exec: java assume PID 1 e recebe SIGTERM direto — sem isso o k8s espera
# o grace period inteiro em cada rolling update antes do SIGKILL.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

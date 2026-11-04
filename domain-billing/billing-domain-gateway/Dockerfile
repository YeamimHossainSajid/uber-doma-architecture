# Build context: repository root (see docker-compose.yml)
FROM maven:3.10-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -q -pl domain-billing/billing-domain-gateway -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/domain-billing/billing-domain-gateway/target/billing-domain-gateway-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

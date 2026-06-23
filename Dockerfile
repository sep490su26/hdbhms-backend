# Stage 1: Build application
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /builder
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Extract Spring Boot layers
FROM eclipse-temurin:21-jdk-alpine AS extractor
WORKDIR /extractor
COPY --from=builder /builder/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 3: Build truststore (JDK, temporary)
FROM eclipse-temurin:21-jdk-alpine AS truststore-builder
WORKDIR /certs
COPY certs/ca.pem .
RUN keytool -importcert \
    -alias aiven-ca \
    -file ca.pem \
    -keystore kafka.truststore.jks \
    -storepass changeit \
    -noprompt

# Stage 4: Slim runtime (JRE, no JDK)
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /application

# Copy the pre‑built truststore (no keytool needed at runtime)
COPY --from=truststore-builder /certs/kafka.truststore.jks /application/certs/kafka.truststore.jks

COPY docker/entrypoint.sh /application/entrypoint.sh
RUN chmod +x /application/entrypoint.sh

RUN addgroup -S spring && adduser -S spring -G spring \
    && chown -R spring:spring /application

USER spring:spring

COPY --from=extractor /extractor/dependencies/ ./
COPY --from=extractor /extractor/spring-boot-loader/ ./
COPY --from=extractor /extractor/snapshot-dependencies/ ./
COPY --from=extractor /extractor/application/ ./

EXPOSE 8080
ENTRYPOINT ["/application/entrypoint.sh"]
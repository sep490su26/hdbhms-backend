# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /builder
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Extract layers
FROM eclipse-temurin:21-jdk-alpine AS extractor
WORKDIR /extractor
COPY --from=builder /builder/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 3: Runtime
FROM eclipse-temurin:21-jdk-alpine AS runtime
WORKDIR /application

COPY certs/ca.pem /application/certs/ca.pem
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
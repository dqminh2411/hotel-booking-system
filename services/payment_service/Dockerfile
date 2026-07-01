# # Build stage
# FROM maven:3.9.11-eclipse-temurin-21-alpine AS builder

# WORKDIR /build


# COPY pom.xml .

# RUN mvn -Dmaven.wagon.http.retryHandler.count=3 \
#         -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
#         -Dmaven.wagon.http.connectionTimeout=120000 \
#         -Dmaven.wagon.http.readTimeout=120000 \
#         dependency:go-offline -DskipTests -q

# COPY src src
# RUN mvn package -DskipTests -q

# # Runtime stage
# FROM eclipse-temurin:21-jre-alpine

# WORKDIR /app

# COPY --from=builder /build/target/payment_service-0.0.1-SNAPSHOT.jar app.jar

# EXPOSE 5000

# CMD ["java", "-jar", "app.jar"]

FROM maven:3.9-eclipse-temurin-21

WORKDIR /app

# COPY pom.xml .
# RUN mvn dependency:go-offline

CMD ["mvn", "spring-boot:run"]
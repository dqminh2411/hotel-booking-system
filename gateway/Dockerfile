# FROM maven:3.9.9-eclipse-temurin-21 AS builder
# WORKDIR /app

# COPY pom.xml pom.xml
# RUN mvn -q -DskipTests dependency:go-offline

# COPY src src
# RUN mvn -q -DskipTests package

# FROM eclipse-temurin:21-jre
# WORKDIR /app
# COPY --from=builder /app/target/*.jar app.jar
# EXPOSE 8000
# ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM maven:3.9-eclipse-temurin-21

WORKDIR /app

# COPY pom.xml .
# RUN mvn dependency:go-offline

CMD ["mvn", "spring-boot:run"]
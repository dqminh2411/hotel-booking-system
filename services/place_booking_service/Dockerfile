# FROM maven:3.9.11-eclipse-temurin-21-alpine AS builder

# WORKDIR /app

# COPY pom.xml .
# RUN mvn dependency:go-offline

# COPY src src

# RUN mvn -DskipTests package

# FROM eclipse-temurin:21-jre-alpine

# WORKDIR /app

# COPY --from=builder /app/target/*.jar app.jar

# EXPOSE 5000

# ENTRYPOINT ["java","-jar","app.jar"]

FROM maven:3.9-eclipse-temurin-21

WORKDIR /app

# COPY pom.xml .
# RUN mvn dependency:go-offline

CMD ["mvn", "spring-boot:run"]
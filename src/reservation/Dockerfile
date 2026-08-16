FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY . .

RUN mvn clean package -DskipTests


FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /app/target/reservation-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081 9091

ENTRYPOINT ["java", "-jar", "app.jar"]
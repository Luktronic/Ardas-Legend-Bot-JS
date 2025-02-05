# Dockerfile for Java backend
# Filename: Dockerfile

# First stage: build the application
FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY . /app
WORKDIR /app
RUN mvn clean
RUN mvn package -DskipTests -f pom.xml

# Second stage: create a slim image
FROM eclipse-temurin:21-jre
ENV SPRING_PROFILES_ACTIVE=production
COPY --from=build /app/target/alspringbackend-1.0.4.jar /alspringbackend-1.0.4.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/alspringbackend-1.0.4.jar"]
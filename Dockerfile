# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only the pom first to leverage Docker layer caching for dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy the rest of the source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Render (and most PaaS) inject PORT; fall back to the port in application.properties
ENV SERVER_PORT=8183
EXPOSE 8183

ENTRYPOINT ["java", "-jar", "app.jar"]

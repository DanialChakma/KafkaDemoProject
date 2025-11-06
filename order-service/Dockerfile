# Step 1: Use an official JDK image
#FROM eclipse-temurin:17-jdk-alpine
FROM eclipse-temurin:17-jre-alpine
# Step 2: Set working directory
WORKDIR /app

# Step 3: Copy the jar file from target/ to container
COPY target/*.jar app.jar

# Step 4: Expose port (same as your Spring Boot app)
EXPOSE 8086

# Step 5: Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]

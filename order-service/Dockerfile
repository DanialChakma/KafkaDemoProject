# ============================
# Stage 1: Build Spring Boot JAR with AOT
# ============================
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy Maven wrapper first (cache dependencies)
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw

# Copy pom.xml separately to leverage Docker cache for dependencies
COPY pom.xml .
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application with AOT
RUN ./mvnw clean package -DskipTests
RUN ./mvnw -Pnative spring-boot:process-aot \
    -Dspring-boot.main-class=com.oms.OrderServiceApplication

# ============================
# Stage 2: Minimal JRE with jlink
# ============================
FROM eclipse-temurin:17-jdk-jammy AS jre-build

RUN $JAVA_HOME/bin/jlink \
    --add-modules java.base,java.logging,java.sql,java.naming,java.management,java.instrument,jdk.unsupported,java.desktop \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --compress=2 \
    --output /jre-min

# ============================
# Stage 3: Distroless Runtime
# ============================
FROM gcr.io/distroless/base-debian11:nonroot

ENV JAVA_HOME=/opt/jre
ENV PATH="$JAVA_HOME/bin:${PATH}"

# Copy minimal JRE
COPY --from=jre-build /jre-min /opt/jre
# Copy built Spring Boot jar
COPY --from=build /app/target/*.jar /app/app.jar

WORKDIR /app
EXPOSE 8086
USER nonroot

# JVM options optimized for low memory usage
#ENV JAVA_OPTS="\
#  -Xms32m \
#  -Xmx64m \
#  -XX:+UseSerialGC \
#  -XX:MaxMetaspaceSize=80m \
#  -XX:+UseStringDeduplication \
#  -XX:MaxDirectMemorySize=32m \
#  -Dio.netty.maxDirectMemory=32m \
#  -Xss256k \
#  -XX:+ExitOnOutOfMemoryError \
#"


ENV JAVA_TOOL_OPTIONS="\
  -Xms32m \
  -Xmx64m \
  -XX:+UseSerialGC \
  -XX:MaxMetaspaceSize=128m \
  -XX:+UseStringDeduplication \
  -XX:MaxDirectMemorySize=32m \
  -Dio.netty.maxDirectMemory=32m \
  -Xss192k \
  -Dio.netty.allocator.numDirectArenas=1 \
  -Dio.netty.allocator.numHeapArenas=1 \
  -XX:+ExitOnOutOfMemoryError \
"
# Run the Spring Boot app
ENTRYPOINT ["java","-jar","/app/app.jar"]

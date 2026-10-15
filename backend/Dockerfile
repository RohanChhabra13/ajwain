FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:17-jre
RUN useradd --system --create-home app && mkdir -p /data/uploads && chown -R app /data
USER app
WORKDIR /app
COPY --from=build /app/target/work-platform-*.jar app.jar
ENV STORAGE_DIR=/data/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

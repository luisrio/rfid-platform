FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
COPY src src

RUN mvn -B -DskipTests clean package
RUN set -eux; JAR="$(find target -maxdepth 1 -type f -name "*.jar" ! -name "original-*.jar" | head -n 1)"; cp "$JAR" target/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/app.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar", "--qos", "0"]

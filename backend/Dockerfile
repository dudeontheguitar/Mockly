FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY mockly-api/pom.xml mockly-api/pom.xml
COPY mockly-core/pom.xml mockly-core/pom.xml
COPY mockly-data/pom.xml mockly-data/pom.xml
COPY mockly-security/pom.xml mockly-security/pom.xml

RUN mvn -B -ntp -pl mockly-api -am dependency:go-offline

COPY mockly-api/src mockly-api/src
COPY mockly-core/src mockly-core/src
COPY mockly-data/src mockly-data/src
COPY mockly-security/src mockly-security/src

RUN mvn -B -ntp -pl mockly-api -am clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/mockly-api/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /src
COPY . .
ARG MODULE
RUN mvn -B -ntp -pl ${MODULE} -am package -DskipTests && cp ${MODULE}/target/${MODULE}-1.0.0-SNAPSHOT.jar /app.jar
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build --chown=10001:10001 /app.jar /app/app.jar
USER 10001:10001
ENTRYPOINT ["java","-jar","/app/app.jar"]

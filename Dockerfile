FROM maven:3.8.3-openjdk-11 AS build

ADD pom.xml /pom.xml
ADD src /src

RUN mvn clean install

FROM openjdk:11.0.13-jdk

COPY --from=build target/babbyconnect-proxy.jar /babbyconnect-proxy.jar

ENV TZ="Europe/Amsterdam"

EXPOSE 8080

ENTRYPOINT [ "sh", "-c", "java -jar /babbyconnect-proxy.jar" ]

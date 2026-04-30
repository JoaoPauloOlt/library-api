From ubuntu:latest AS build

run apt-get update
run apt-get install openjdk-17-jdk -y
copy . .

run apt-get install maven -y
run mvn clean install

from openjdk:17-jdk-slim
EXPOSE 3306

COPY --from=build /target/library-api-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT [ "java", "-jar", "app.jar"]
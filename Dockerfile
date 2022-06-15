FROM diyfr/openjdk8-alpine-fonts
ADD target/lazadalisting-1.0-SNAPSHOT.jar target/lazadalisting-1.0-SNAPSHOT.jar
ADD src src
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "target/lazadalisting-1.0-SNAPSHOT.jar"]
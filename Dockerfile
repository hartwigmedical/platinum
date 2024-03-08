FROM google/cloud-sdk:latest

RUN apt update
RUN apt --yes install openjdk-11-jre kubectl

ADD platinum platinum
ADD target/platinum.jar /target/platinum-local-SNAPSHOT.jar
ADD target/lib /target/lib

ENTRYPOINT ["./platinum"]

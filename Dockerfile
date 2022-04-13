FROM google/cloud-sdk:latest

RUN apt update
RUN apt --yes install openjdk-11-jre kubectl google-cloud-sdk 

ADD platinum platinum
ADD target/platinum-local-SNAPSHOT.jar /target/platinum-local-SNAPSHOT.jar
ADD target/lib /target/lib

ENTRYPOINT ["./platinum"]

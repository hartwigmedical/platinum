FROM google/cloud-sdk:latest

RUN apt update
RUN apt --yes install openjdk-11-jre kubectl google-cloud-sdk 

ADD platinum platinum
ARG VERSION
ADD target/platinum-$VERSION.jar /target/platinum.jar
ADD target/lib /target/lib

ENTRYPOINT ["./platinum"]

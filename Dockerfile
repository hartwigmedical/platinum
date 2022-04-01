FROM google/cloud-sdk:latest

RUN apt update
RUN apt --yes install openjdk-11-jre

ADD bin/platinum.sh platinum.sh
ADD target/lib /usr/share/platinum/lib
ADD target/platinum-local-SNAPSHOT.jar /usr/share/platinum/bootstrap.jar

ENTRYPOINT ["./platinum.sh"]

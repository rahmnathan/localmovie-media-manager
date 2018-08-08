FROM ubuntu:18.04

MAINTAINER nathan

RUN apt-get update && \
    apt-get -y install apt-utils ffmpeg handbrake-cli software-properties-common openjdk-8-jre && \
    apt-get clean

RUN mkdir /opt/localmovie && mkdir /opt/localmovie/config

# Java 10 install logic
#    add-apt-repository -y ppa:linuxuprising/java && \
#    yes | apt-get -y install oracle-java10-installer && \
#    apt-get -y install gnupg oracle-java10-set-default

ADD src/main/resources/vault.cer /opt/localmovie/vault.cer
RUN keytool -importcert -file /opt/localmovie/vault.cer -keystore /usr/lib/jvm/java-8-openjdk-amd64/jre/lib/security/cacerts -storepass changeit -noprompt -alias "vault"

ARG JAR_FILE
ADD target/$JAR_FILE /opt/localmovie/localmovie-media-manager.jar

WORKDIR /opt/localmovie/
ENTRYPOINT java -jar localmovie-media-manager.jar
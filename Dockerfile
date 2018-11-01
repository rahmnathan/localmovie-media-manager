FROM ubuntu:18.04

MAINTAINER nathan

RUN apt-get update && \
    apt-get -y install apt-utils ffmpeg handbrake-cli software-properties-common wget tar && \
    apt-get clean

RUN mkdir /opt/localmovie && mkdir /opt/localmovie/config

# Java 10 install logic
#RUN add-apt-repository -y ppa:linuxuprising/java && \
#    yes | apt-get -y install oracle-java10-installer && \
#    apt-get -y install gnupg oracle-java10-set-default

# Java 11 install logic
RUN wget "https://download.java.net/openjdk/jdk11/ri/openjdk-11+28_linux-x64_bin.tar.gz" && \
    tar -zxvf openjdk-11+28_linux-x64_bin.tar.gz && \
    mv jdk-11 /usr/lib/jvm && \
    rm -f openjdk-11+28_linux-x64_bin.tar.gz

ADD src/main/resources/vault.cer /opt/localmovie/vault.cer
RUN /usr/lib/jvm/jdk-11/bin/keytool -importcert -file /opt/localmovie/vault.cer -keystore /usr/lib/jvm/jdk-11/lib/security/cacerts -storepass changeit -noprompt -alias "vault"

ARG JAR_FILE
ADD target/$JAR_FILE /opt/localmovie/localmovie-media-manager.jar

WORKDIR /opt/localmovie/
ENTRYPOINT java -jar localmovie-media-manager.jar
FROM ubuntu:18.04

MAINTAINER nathan

RUN apt-get update && \
    apt-get -y install apt-utils ffmpeg handbrake-cli software-properties-common wget tar && \
    apt-get clean

RUN mkdir /opt/localmovie && mkdir /opt/localmovie/config

# Java 10 install logic
RUN wget "https://download.java.net/java/GA/jdk10/10.0.2/19aef61b38124481863b1413dce1855f/13/openjdk-10.0.2_linux-x64_bin.tar.gz" && \
    tar -zxvf openjdk-10.0.2_linux-x64_bin.tar.gz && \
    rm -f openjdk-10.0.2_linux-x64_bin.tar.gz

ENV PATH /jdk-10.0.2/bin:$PATH

# Java 11 install logic
#RUN wget "https://download.java.net/openjdk/jdk11/ri/openjdk-11+28_linux-x64_bin.tar.gz" && \
#    tar -zxvf openjdk-11+28_linux-x64_bin.tar.gz && \
#    mv jdk-11 /usr/lib/jvm && \
#    rm -f openjdk-11+28_linux-x64_bin.tar.gz

ADD src/main/resources/vault.cer /opt/localmovie/vault.cer
RUN keytool -importcert -file /opt/localmovie/vault.cer -keystore jdk-10.0.2/lib/security/cacerts -storepass changeit -noprompt -alias "vault"

ARG JAR_FILE
ADD target/$JAR_FILE /opt/localmovie/localmovie-media-manager.jar

WORKDIR /opt/localmovie/
ENTRYPOINT java -jar localmovie-media-manager.jar
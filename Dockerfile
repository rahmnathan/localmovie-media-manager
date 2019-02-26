FROM ubuntu:18.04

MAINTAINER nathan

RUN apt-get update && \
    apt-get -y install apt-utils ffmpeg handbrake-cli software-properties-common wget tar && \
    apt-get clean

RUN mkdir /opt/localmovie && mkdir /opt/localmovie/config

# Java 11 install logic
RUN wget "https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.2%2B9/OpenJDK11U-jdk_x64_linux_hotspot_11.0.2_9.tar.gz" && \
    tar -zxvf OpenJDK11U-jdk_x64_linux_hotspot_11.0.2_9.tar.gz && \
    mv jdk-11.0.2+9 /usr/lib/jvm && \
    rm -f OpenJDK11U-jdk_x64_linux_hotspot_11.0.2_9.tar.gz

ENV JAVA_HOME /usr/lib/jvm
ENV PATH $JAVA_HOME/bin:$PATH

ADD src/main/resources/vault.cer /opt/localmovie/vault.cer
RUN keytool -importcert -file /opt/localmovie/vault.cer -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt -alias "vault"

ADD src/main/resources/google.cer /opt/localmovie/google.cer
RUN keytool -importcert -file /opt/localmovie/google.cer -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt -alias "google"

ARG JAR_FILE
ADD target/$JAR_FILE /opt/localmovie/localmovie-media-manager.jar

WORKDIR /opt/localmovie/
ENTRYPOINT java -jar localmovie-media-manager.jar
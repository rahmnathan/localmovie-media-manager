FROM adoptopenjdk/openjdk11:slim

MAINTAINER nathan

RUN apt-get update && \
    apt-get -y install apt-utils ffmpeg handbrake-cli software-properties-common wget tar && \
    apt-get clean

RUN mkdir /opt/localmovie && mkdir /opt/localmovie/config

ADD src/main/resources/vault.cer /opt/localmovie/vault.cer
RUN keytool -importcert -file /opt/localmovie/vault.cer -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt -alias "vault"

ADD src/main/resources/google.cer /opt/localmovie/google.cer
RUN keytool -importcert -file /opt/localmovie/google.cer -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt -alias "google"

ARG JAR_FILE
ADD target/$JAR_FILE /opt/localmovie/localmovie-media-manager.jar

WORKDIR /opt/localmovie/
ENTRYPOINT java -jar localmovie-media-manager.jar
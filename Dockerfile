FROM openjdk:17-slim

RUN apt-get update && \
    apt-get -y install apt-utils ffmpeg handbrake-cli software-properties-common wget tar && \
    apt-get clean

RUN groupadd localmovie && useradd localmovie -g localmovie && mkdir -p /opt/localmovie/config

ARG JAR_FILE
ADD target/$JAR_FILE /opt/localmovie/localmovie-media-manager.jar

RUN chown -R localmovie:localmovie /opt/localmovie

USER localmovie

WORKDIR /opt/localmovie/
ENTRYPOINT java -jar localmovie-media-manager.jar
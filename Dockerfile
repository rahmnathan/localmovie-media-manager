FROM eclipse-temurin:21-jre

RUN groupadd localmovie && useradd localmovie -g localmovie && mkdir -p /opt/localmovie/config

ARG JAR_FILE
ADD target/$JAR_FILE /opt/localmovie/localmovie-media-manager.jar

RUN chown -R localmovie:localmovie /opt/localmovie

USER localmovie

WORKDIR /opt/localmovie/
ENTRYPOINT java -jar localmovie-media-manager.jar
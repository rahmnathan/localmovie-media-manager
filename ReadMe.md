<h1>LocalMovies</h1>

[![Build Status](https://jenkins.nathanrahm.com/buildStatus/icon?job=localmovie-media-manager)](https://jenkins.nathanrahm.com/job/localmovie-media-manager/)

This repository is a Spring Boot video streaming service for my media collection.

<h4>User Interface</h4>
There are two user interfaces available.

- [Android Application](https://play.google.com/store/apps/details?id=rahm.nathan.localmovies&hl=en)

- [ReactJS web application](https://movies.nathanrahm.com) (located in the src/main/js directory).

<h4>APIs</h4>
This service exposes a set of endpoints that facilitate:
- Loading media at a given path ('/Movies', '/Series', etc).
- Streaming a media file.
- Loading media events (for persistent Android clients).

When new media is added to a monitored directory, the following process is triggered:
 - Media is converted to H.264/AAC format (if necessary).
 - Media metadata is downloaded from OMDB and stored in database.
 - 'New Media' event is added to database for processing by Android clients.
 - Push notifications are sent to Android devices to notify of new media.
 
 <h4>Tech Stack</h4>
 - Spring Boot
 - Apache Camel
 - Maven
 - Postgresql
 - Hashicorp Vault
 - Keycloak
 
 <h4>CI/CD</h4>
 Jenkins pipeline is located in the jenkins.groovy file.
 
 <h4>Monitoring</h4>
 Monitoring is facilitated by Prometheus and Grafana.

<h4>Deployment</h4>
The backend system is deployed in an on-prem Kubernetes instance. Helm charts for Kubernetes deployment are located in 
the helm/localmovies directory.

<h4>System Diagram</h4>

![Imgur Image](https://imgur.com/hA5ur36.png)
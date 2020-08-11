<h1>LocalMovies</h1>

[![Build Status](https://jenkins.nathanrahm.com/buildStatus/icon?job=localmovie-media-manager)](https://jenkins.nathanrahm.com/job/localmovie-media-manager/)

This repository is a Spring Boot video streaming service for my media collection.

<h4>Demo</h4>
You can view a brief demo of this project [here](https://nathanrahm.com/projects).

<h4>User Interface</h4>

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
 - [Hashicorp Vault](https://vault.nathanrahm.com)
 - [Keycloak](https://login.nathanrahm.com/)
 
 <h4>Monitoring</h4>
 
 - [Grafana](https://grafana.nathanrahm.com/d/kjj-uuJWk/localmovies?orgId=1)
 - Prometheus

<h4>CI/CD</h4>

- [Kubernetes](https://kube.nathanrahm.com)
- [Jenkins](https://jenkins.nathanrahm.com)
- Helm (chart located in helm/localmovies directory)

<h4>System Diagram</h4>

![Imgur Image](https://imgur.com/hA5ur36.png)
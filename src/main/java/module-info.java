module local.movie.service {
    requires spring.context;
    requires spring.beans;
    requires guava;
    requires slf4j.api;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.module.paramnames;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires spring.boot;
    requires ffmpeg;
    requires micrometer.core;
    requires google.pushnotification;
    requires spring.web;
    requires camel.core;
    requires spring.cloud.vault.config;
    requires spring.data.jpa;
    requires spring.data.commons;
    requires spring.boot.autoconfigure;
    requires spring.vault.core;
    requires jackson.annotations;
    requires movie.info.omdb;
    requires directory.monitor;
    requires cast.video.converter.handbrake;
    requires java.sql;
    requires spring.tx;
    requires java.persistence;
    requires commons.lang;
    requires spring.core;
    requires org.apache.httpcomponents.httpcore;
    requires com.fasterxml.jackson.core;
    requires spring.security.core;
    requires tomcat.embed.core;
}

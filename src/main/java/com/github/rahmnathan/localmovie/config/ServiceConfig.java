package com.github.rahmnathan.localmovie.config;

import com.github.rahmnathan.localmovie.data.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.temporal.ChronoUnit;

@Configuration
@ConfigurationProperties(prefix = "service")
public class ServiceConfig {
    private MediaEventMonitorConfig directoryMonitor;
    private MediaRepositoryMonitorConfig repository;
    private boolean notificationsEnabled;
    private String[] mediaPaths;
    private String omdbApiKey;
    private String jedisHost;

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public String getOmdbApiKey() {
        return omdbApiKey;
    }

    public void setOmdbApiKey(String omdbApiKey) {
        this.omdbApiKey = omdbApiKey;
    }

    public String getJedisHost() {
        return jedisHost;
    }

    public void setJedisHost(String jedisHost) {
        this.jedisHost = jedisHost;
    }

    public String[] getMediaPaths() {
        return mediaPaths;
    }

    public void setMediaPaths(String[] mediaPaths) {
        this.mediaPaths = mediaPaths;
    }

    public MediaEventMonitorConfig getDirectoryMonitor() {
        return directoryMonitor;
    }

    public MediaRepositoryMonitorConfig getRepository() {
        return repository;
    }

    public void setDirectoryMonitor(MediaEventMonitorConfig directoryMonitor) {
        this.directoryMonitor = directoryMonitor;
    }

    public void setRepository(MediaRepositoryMonitorConfig repository) {
        this.repository = repository;
    }

    public static class MediaRepositoryMonitorConfig {
        private Duration updateFrequency = new Duration(ChronoUnit.DAYS, 3L);
        private int updateLimit = 200;

        public Duration getUpdateFrequency() {
            return updateFrequency;
        }

        public void setUpdateFrequency(Duration updateFrequency) {
            this.updateFrequency = updateFrequency;
        }

        public int getUpdateLimit() {
            return updateLimit;
        }

        public void setUpdateLimit(int updateLimit) {
            this.updateLimit = updateLimit;
        }
    }

    public static class MediaEventMonitorConfig {
        private String ffprobeLocation = "/usr/bin/ffprobe";
        private int concurrentConversionLimit = 3;

        public String getFfprobeLocation() {
            return ffprobeLocation;
        }

        public void setFfprobeLocation(String ffprobeLocation) {
            this.ffprobeLocation = ffprobeLocation;
        }

        public int getConcurrentConversionLimit() {
            return concurrentConversionLimit;
        }

        public void setConcurrentConversionLimit(int concurrentConversionLimit) {
            this.concurrentConversionLimit = concurrentConversionLimit;
        }
    }
}

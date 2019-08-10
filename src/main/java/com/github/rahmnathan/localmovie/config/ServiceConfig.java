package com.github.rahmnathan.localmovie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
        private int updateFrequencyDays = 3;
        private int updateLimit = 200;

        public int getUpdateFrequencyDays() {
            return updateFrequencyDays;
        }

        public void setUpdateFrequencyDays(int updateFrequencyDays) {
            this.updateFrequencyDays = updateFrequencyDays;
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

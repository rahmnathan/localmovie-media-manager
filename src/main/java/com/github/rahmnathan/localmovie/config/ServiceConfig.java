package com.github.rahmnathan.localmovie.config;

import com.github.rahmnathan.localmovie.data.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.temporal.ChronoUnit;
import java.util.Set;

@Data
@Configuration
@ConfigurationProperties(prefix = "service")
public class ServiceConfig {
    private MediaEventMonitorConfig directoryMonitor;
    private MediaRepositoryMonitorConfig repository;
    private ConversionServiceConfig conversionService;
    private boolean notificationsEnabled;
    private Set<String> mediaPaths;
    private String jedisHost;
    private OmdbConfig omdb;
    private OpenSubtitlesConfig opensubtitles;
    private OllamaConfig ollama;
    private boolean forceConvert;

    @Data
    public static class MediaRepositoryMonitorConfig {
        private Duration updateFrequency = new Duration(ChronoUnit.DAYS, 3L);
        private int updateLimit = 200;
    }

    @Data
    public static class MediaEventMonitorConfig {
        private String ffprobeLocation = "/usr/bin/ffprobe";
        private int concurrentConversionLimit = 3;
    }

    @Data
    public static class ConversionServiceConfig {
        private boolean enabled;
    }

    @Data
    public static class OmdbConfig {
        private boolean enabled;
        private String apiKey;
    }

    @Data
    public static class OpenSubtitlesConfig {
        private boolean enabled;
        private String apiKey;
        private String username;
        private String password;
    }

    @Data
    public static class OllamaConfig {
        private boolean enabled;
        private String baseUrl = "http://open-webui-ollama.open-webui.svc.cluster.local:11434";
        private String model = "gpt-oss:20b";
    }
}

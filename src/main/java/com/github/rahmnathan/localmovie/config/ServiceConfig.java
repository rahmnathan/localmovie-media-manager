package com.github.rahmnathan.localmovie.config;

import com.github.rahmnathan.localmovie.data.Duration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.temporal.ChronoUnit;
import java.util.Set;

@Data
@Configuration
@Validated
@ConfigurationProperties(prefix = "service")
public class ServiceConfig {
    private MediaEventMonitorConfig directoryMonitor;
    private MediaRepositoryMonitorConfig repository;
    private ConversionServiceConfig conversionService;
    private boolean notificationsEnabled;
    private Set<String> mediaPaths;
    private String jedisHost;
    private OmdbConfig omdb;
    @Valid
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
        private int concurrentConversionLimit = 3;
    }

    @Data
    public static class ConversionServiceConfig {
        private boolean enabled;
        private String encoder = "HANDBRAKE";
    }

    @Data
    public static class OmdbConfig {
        private boolean enabled;
        private String apiKey;
    }

    @Data
    public static class OpenSubtitlesConfig {
        private boolean enabled;
        private boolean syncEnabled;
        @NotBlank
        private String syncImage = "ghcr.io/smacke/ffsubsync:latest";
        @Min(1)
        private int syncTimeoutSeconds = 300;
        @Min(1)
        private int syncMaxOffsetSeconds = 120;
        @Min(1)
        private int staleRunningJobTimeoutMinutes = 60;
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

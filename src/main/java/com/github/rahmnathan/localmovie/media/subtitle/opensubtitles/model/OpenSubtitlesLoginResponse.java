package com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenSubtitlesLoginResponse {
    private String token;
    private UserInfo user;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserInfo {
        @JsonProperty("allowed_downloads")
        private int allowedDownloads;
        @JsonProperty("downloads_count")
        private int downloadsCount;
    }
}

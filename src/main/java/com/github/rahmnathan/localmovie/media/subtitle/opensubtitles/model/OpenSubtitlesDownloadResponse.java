package com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenSubtitlesDownloadResponse {
    private String link;
    @JsonProperty("file_name")
    private String fileName;
    private int remaining;
}

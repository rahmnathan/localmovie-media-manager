package com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenSubtitlesSearchResponse {
    @JsonProperty("total_count")
    private int totalCount;
    private List<SubtitleData> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubtitleData {
        private String id;
        private SubtitleAttributes attributes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubtitleAttributes {
        @JsonProperty("subtitle_id")
        private String subtitleId;
        private String language;
        @JsonProperty("download_count")
        private int downloadCount;
        private List<SubtitleFile> files;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubtitleFile {
        @JsonProperty("file_id")
        private int fileId;
        @JsonProperty("file_name")
        private String fileName;
    }

    public boolean hasResults() {
        return data != null && !data.isEmpty();
    }
}

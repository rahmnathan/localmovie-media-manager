package com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OpenSubtitlesDownloadRequest {
    @JsonProperty("file_id")
    private int fileId;
    @JsonProperty("sub_format")
    private String subFormat;
}

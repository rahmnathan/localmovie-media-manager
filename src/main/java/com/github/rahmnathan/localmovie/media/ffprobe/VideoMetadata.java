package com.github.rahmnathan.localmovie.media.ffprobe;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoMetadata {
    private String videoCodec;
    private String audioCodec;
    private String containerFormat;
    private Long durationSeconds;
    private Integer bitrateKbps;
    private String resolution;
}

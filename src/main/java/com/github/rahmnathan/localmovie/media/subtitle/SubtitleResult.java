package com.github.rahmnathan.localmovie.media.subtitle;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubtitleResult {
    private String content;
    private String opensubtitlesId;
    private String languageCode;
    private String format;
}

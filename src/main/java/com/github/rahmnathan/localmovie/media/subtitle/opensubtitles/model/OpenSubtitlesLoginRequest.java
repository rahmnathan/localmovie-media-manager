package com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OpenSubtitlesLoginRequest {
    private String username;
    private String password;
}

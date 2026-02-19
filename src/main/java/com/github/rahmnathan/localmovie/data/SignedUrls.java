package com.github.rahmnathan.localmovie.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignedUrls {
    private final String stream;
    private final String streamContentType;
    private final String poster;
    private final String updatePosition;
    private final String subtitle;
}

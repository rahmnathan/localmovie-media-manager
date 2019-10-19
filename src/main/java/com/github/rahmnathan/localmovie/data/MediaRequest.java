package com.github.rahmnathan.localmovie.data;

import lombok.Value;

@Value
public class MediaRequest {
    private final String path;
    private final Integer page;
    private final Integer resultsPerPage;
    private final MediaClient client;
    private final MediaOrder order;
}

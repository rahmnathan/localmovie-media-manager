package com.github.rahmnathan.localmovie.data;

import lombok.NonNull;
import lombok.Value;

@Value
public class MediaRequest {
    @NonNull
    private final String path;
    private final Integer page;
    private final Integer resultsPerPage;
    @NonNull
    private final MediaClient client;
    private final MediaOrder order;
}

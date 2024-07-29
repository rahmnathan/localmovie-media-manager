package com.github.rahmnathan.localmovie.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class MediaRequest {
    @NonNull
    private final String path;
    private int page = 0;
    private int pageSize = 50;
    private final String order;
    private String q;
    private final String genre;
}

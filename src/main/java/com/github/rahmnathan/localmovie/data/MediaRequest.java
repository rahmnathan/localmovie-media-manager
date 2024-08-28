package com.github.rahmnathan.localmovie.data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MediaRequest {
    @Max(100)
    private final String path;
    @Max(100)
    private int page;
    @Min(1)
    @Max(100)
    private int pageSize;
    @Max(50)
    private final String order;
    @Max(50)
    private String q;
    @Max(50)
    private final String genre;
    @Max(20)
    private final String type;
}

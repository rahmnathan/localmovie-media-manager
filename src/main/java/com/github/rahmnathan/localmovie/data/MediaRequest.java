package com.github.rahmnathan.localmovie.data;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MediaRequest {
    private final String path;
    private int page;
    @Min(value = 1, message = "The value must be positive")
    private int pageSize;
    private final String order;
    private String q;
    private final String genre;
}

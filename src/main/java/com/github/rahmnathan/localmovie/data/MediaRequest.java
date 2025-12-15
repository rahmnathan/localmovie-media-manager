package com.github.rahmnathan.localmovie.data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MediaRequest {
    @Size(max = 100)
    private final String path;
    @Size(max = 50)
    private String mediaType;
    @Size(max = 100)
    private String mediaId;
    @Max(100)
    private int page;
    @Min(1)
    @Max(100)
    private int pageSize;
    @Size(max = 50)
    private final String order;
    @Size(max = 50)
    private String q;
    @Size(max = 50)
    private final String genre;
    @Size(max = 20)
    private final String type;
}

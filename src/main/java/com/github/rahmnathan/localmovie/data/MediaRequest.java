package com.github.rahmnathan.localmovie.data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MediaRequest {
    @Size(max = 100)
    private String path;
    @Size(max = 100)
    private String parentId;
    @Max(100)
    private int page;
    @Min(1)
    @Max(100)
    private int pageSize;
    @Size(max = 50)
    private String order;
    @Size(max = 50)
    private String q;
    @Size(max = 50)
    private String genre;
    @Size(max = 20)
    private String type;
    @Size(max = 20)
    private String client;
    private Boolean includeDetails;

    public MediaRequest(String path, String parentId, int page, int pageSize, String order, String q, String genre, String type) {
        this(path, parentId, page, pageSize, order, q, genre, type, null, null);
    }

    public MediaRequest(String path, String parentId, int page, int pageSize, String order, String q, String genre, String type, String client, Boolean includeDetails) {
        this.path = path;
        this.parentId = parentId;
        this.page = page;
        this.pageSize = pageSize;
        this.order = order;
        this.q = q;
        this.genre = genre;
        this.type = type;
        this.client = client;
        this.includeDetails = includeDetails;
    }
}

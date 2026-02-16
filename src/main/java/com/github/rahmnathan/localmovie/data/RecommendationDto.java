package com.github.rahmnathan.localmovie.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecommendationDto {
    private MediaFileDto mediaFile;
    private String reason;
    private Integer rank;
}

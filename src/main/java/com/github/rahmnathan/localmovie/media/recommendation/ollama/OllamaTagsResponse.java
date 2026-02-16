package com.github.rahmnathan.localmovie.media.recommendation.ollama;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class OllamaTagsResponse {
    private List<OllamaModel> models;

    @Data
    @NoArgsConstructor
    public static class OllamaModel {
        private String name;
        private String model;
    }
}

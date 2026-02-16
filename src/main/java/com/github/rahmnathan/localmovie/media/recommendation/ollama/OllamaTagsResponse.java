package com.github.rahmnathan.localmovie.media.recommendation.ollama;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaTagsResponse {
    private List<OllamaModel> models;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OllamaModel {
        private String name;
        private String model;
    }
}

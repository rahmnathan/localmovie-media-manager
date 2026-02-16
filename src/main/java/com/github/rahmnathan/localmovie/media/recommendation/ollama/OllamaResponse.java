package com.github.rahmnathan.localmovie.media.recommendation.ollama;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaResponse {
    private String model;
    private String response;
    private boolean done;
}

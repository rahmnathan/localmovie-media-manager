package com.github.rahmnathan.localmovie.media.recommendation.ollama;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OllamaResponse {
    private String model;
    private String response;
    private boolean done;
}

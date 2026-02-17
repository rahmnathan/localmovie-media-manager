package com.github.rahmnathan.localmovie.media.recommendation.ollama;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class OllamaRequest {
    private String model;
    private String prompt;
    private boolean stream;
    private String format;
    private Map<String, Object> options;
}

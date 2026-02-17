package com.github.rahmnathan.localmovie.media.recommendation.ollama;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class OllamaRequest {
    private String model;
    private String prompt;
    private String system;
    private boolean stream;
    private String format;
    private Object think;
    private Map<String, Object> options;
}

package com.github.rahmnathan.localmovie.media.recommendation.ollama;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaResponse {
    private String model;
    @JsonAlias({"content", "text", "output"})
    private String response;
    private String thinking;
    private String error;
    @JsonProperty("done_reason")
    private String doneReason;
    private boolean done;
    private OllamaMessage message;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OllamaMessage {
        private String role;
        private String content;
    }
}

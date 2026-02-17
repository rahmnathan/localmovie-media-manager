package com.github.rahmnathan.localmovie.media.recommendation.ollama;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class OllamaClient {
    private static final Logger logger = LoggerFactory.getLogger(OllamaClient.class);

    private final ServiceConfig serviceConfig;
    private final OllamaApi ollamaApi;

    public OllamaClient(ServiceConfig serviceConfig, Optional<OllamaApi> ollamaApi) {
        this.serviceConfig = serviceConfig;
        this.ollamaApi = ollamaApi.orElse(null);
    }

    public String generate(String prompt) {
        ServiceConfig.OllamaConfig ollamaConfig = serviceConfig.getOllama();
        if (ollamaConfig == null || !ollamaConfig.isEnabled() || ollamaApi == null) {
            logger.info("Ollama is disabled or not configured, skipping generation");
            return null;
        }

        try {
            logger.info("Sending request to Ollama with model: {}", ollamaConfig.getModel());

            OllamaRequest request = OllamaRequest.builder()
                    .model(ollamaConfig.getModel())
                    .prompt(prompt)
                    .stream(false)
                    .format("json")
                    .options(Map.of(
                            "temperature", 0.2,
                            "top_p", 0.9
                    ))
                    .build();

            OllamaResponse response = ollamaApi.generate(request);

            if (response != null && response.getResponse() != null) {
                logger.info("Ollama response received, length: {}", response.getResponse().length());
                return response.getResponse();
            } else {
                logger.warn("Ollama returned empty response");
                return null;
            }
        } catch (Exception e) {
            logger.error("Error calling Ollama API", e);
            return null;
        }
    }

    public boolean isAvailable() {
        ServiceConfig.OllamaConfig ollamaConfig = serviceConfig.getOllama();
        if (ollamaConfig == null || !ollamaConfig.isEnabled() || ollamaApi == null) {
            return false;
        }

        try {
            OllamaTagsResponse response = ollamaApi.getTags();
            return response != null;
        } catch (Exception e) {
            logger.warn("Ollama not available: {}", e.getMessage());
            return false;
        }
    }
}

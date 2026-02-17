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
            String modelName = ollamaConfig.getModel() != null ? ollamaConfig.getModel().toLowerCase() : "";
            Object thinkMode = modelName.contains("gpt-oss") ? "low" : Boolean.FALSE;

            OllamaRequest strictRequest = OllamaRequest.builder()
                    .model(ollamaConfig.getModel())
                    .system("Return only the requested JSON. Do not include chain-of-thought, planning, or commentary.")
                    .prompt(prompt)
                    .stream(false)
                    .format("json")
                    .think(thinkMode)
                    .options(Map.of(
                            "num_ctx", 8192,
                            "num_predict", 800,
                            "temperature", 0.2,
                            "top_p", 0.9
                    ))
                    .build();

            String strictResponse = sendGenerate(strictRequest, "strict");
            if (strictResponse != null && !strictResponse.isBlank()) {
                return strictResponse;
            }

            logger.warn("Ollama strict response was empty, retrying with relaxed settings");
            OllamaRequest relaxedRequest = OllamaRequest.builder()
                    .model(ollamaConfig.getModel())
                    .prompt(prompt)
                    .stream(false)
                    .options(Map.of(
                            "num_ctx", 8192,
                            "num_predict", 1200,
                            "temperature", 0.3,
                            "top_p", 0.95
                    ))
                    .build();

            String relaxedResponse = sendGenerate(relaxedRequest, "relaxed");
            if (relaxedResponse != null && !relaxedResponse.isBlank()) {
                return relaxedResponse;
            }

            logger.warn("Ollama returned empty response in both strict and relaxed modes");
            return null;
        } catch (Exception e) {
            logger.error("Error calling Ollama API", e);
            return null;
        }
    }

    private String sendGenerate(OllamaRequest request, String mode) {
        OllamaResponse response = ollamaApi.generate(request);
        if (response == null || response.getResponse() == null) {
            logger.warn("Ollama {} response was null", mode);
            return null;
        }

        String body = response.getResponse().trim();
        logger.info("Ollama {} response received, length: {}", mode, body.length());
        return body;
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

package com.github.rahmnathan.localmovie.media.recommendation.ollama;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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
            Object thinkMode = Boolean.FALSE;

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
                    .system("Return only valid output for the prompt.")
                    .prompt(prompt)
                    .stream(false)
                    .think(thinkMode)
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

            logger.warn("Ollama relaxed response was empty, retrying with minimal settings");
            OllamaRequest recoveryRequest = OllamaRequest.builder()
                    .model(ollamaConfig.getModel())
                    .prompt(prompt)
                    .stream(false)
                    .think(Boolean.FALSE)
                    .options(Map.of(
                            "num_predict", 512,
                            "temperature", 0.1
                    ))
                    .build();

            String recoveryResponse = sendGenerate(recoveryRequest, "recovery");
            if (recoveryResponse != null && !recoveryResponse.isBlank()) {
                return recoveryResponse;
            }

            logger.warn("Ollama returned empty response in strict, relaxed, and recovery modes");
            return null;
        } catch (Exception e) {
            logger.error("Error calling Ollama API", e);
            return null;
        }
    }

    private String sendGenerate(OllamaRequest request, String mode) {
        OllamaResponse response = ollamaApi.generate(request);
        if (response == null) {
            logger.warn("Ollama {} response was null", mode);
            return null;
        }

        if (response.getError() != null && !response.getError().isBlank()) {
            logger.warn("Ollama {} response returned error: {}", mode, response.getError());
        }

        String responseBody = safeTrim(response.getResponse());
        String messageBody = response.getMessage() == null ? "" : safeTrim(response.getMessage().getContent());
        String thinkingBody = safeTrim(response.getThinking());

        String body = firstNonBlank(responseBody, messageBody, thinkingBody);

        if (responseBody.isBlank() && !messageBody.isBlank()) {
            logger.warn("Ollama {} response field was empty but message.content was present (len={}), using message content fallback",
                    mode, messageBody.length());
        } else if (responseBody.isBlank() && messageBody.isBlank() && !thinkingBody.isBlank()) {
            logger.warn("Ollama {} response body was empty but thinking was present (len={}), using thinking fallback",
                    mode, thinkingBody.length());
        }

        logger.info("Ollama {} response received, length: {}, done={}, doneReason={}",
                mode, body.length(), response.isDone(), response.getDoneReason());
        return body;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String... values) {
        return Stream.of(values).filter(v -> v != null && !v.isBlank()).findFirst().orElse("");
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

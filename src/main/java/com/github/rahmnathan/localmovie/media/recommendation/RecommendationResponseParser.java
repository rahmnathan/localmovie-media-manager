package com.github.rahmnathan.localmovie.media.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RecommendationResponseParser {
    private static final Pattern SIMPLE_LINE_PATTERN = Pattern.compile(
            "^(?:[-*]|\\d+[.)]|\\[\\d+])?\\s*([A-Za-z0-9._:-]{4,})\\s*[|\\-\\u2013\\u2014:]\\s*(.+)$");
    private static final Pattern ID_KEY_VALUE_PATTERN = Pattern.compile(
            "(?i)\\b(?:id|mediafileid|media_id)\\s*[:=]\\s*([A-Za-z0-9._:-]{4,})\\b");
    private static final Pattern UUID_ANYWHERE_PATTERN = Pattern.compile(
            "([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})",
            Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;

    @Autowired
    public RecommendationResponseParser(ObjectProvider<ObjectMapper> objectMapperProvider) {
        this.objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
    }

    RecommendationResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<ParsedRecommendation> parseResponse(String response, List<MediaFile> candidates, int maxRecommendations) {
        log.debug("Ollama raw response:\n{}", response);
        Map<String, MediaFile> candidateMap = candidates.stream()
                .filter(mf -> mf.getMediaFileId() != null)
                .collect(Collectors.toMap(mf -> mf.getMediaFileId().toLowerCase(Locale.ROOT), mf -> mf, (a, b) -> a));

        List<ParsedRecommendation> fromJson = parseJsonRecommendations(response, candidateMap);
        if (!fromJson.isEmpty()) {
            return fromJson;
        }

        List<ParsedRecommendation> fromAnyIdMention = parseByIdMentions(response, candidateMap, maxRecommendations);
        if (!fromAnyIdMention.isEmpty()) {
            return fromAnyIdMention;
        }

        List<ParsedRecommendation> fromTitleMentions = parseByTitleMentions(response, candidates, maxRecommendations);
        if (!fromTitleMentions.isEmpty()) {
            return fromTitleMentions;
        }

        List<ParsedRecommendation> results = new ArrayList<>();
        for (String line : response.split("\n")) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }

            Matcher simpleLineMatcher = SIMPLE_LINE_PATTERN.matcher(trimmedLine);
            if (simpleLineMatcher.find()) {
                addResultIfCandidateExists(results, candidateMap, simpleLineMatcher.group(1), simpleLineMatcher.group(2));
                continue;
            }

            Matcher keyValueMatcher = ID_KEY_VALUE_PATTERN.matcher(trimmedLine);
            if (keyValueMatcher.find()) {
                String reason = trimmedLine.replaceFirst("(?i).*\\b(?:reason|because)\\s*[:=]\\s*", "");
                addResultIfCandidateExists(results, candidateMap, keyValueMatcher.group(1), reason);
            }
        }

        if (results.isEmpty()) {
            log.warn("Failed to parse any recommendations from Ollama response. First 500 chars: {}",
                    response.substring(0, Math.min(500, response.length())));
        }

        return results;
    }

    private List<ParsedRecommendation> parseByIdMentions(String response,
                                                         Map<String, MediaFile> candidateMap,
                                                         int maxRecommendations) {
        List<ParsedRecommendation> results = new ArrayList<>();

        Matcher matcher = UUID_ANYWHERE_PATTERN.matcher(response);
        while (matcher.find()) {
            String mediaFileId = matcher.group(1);
            MediaFile candidate = candidateMap.get(mediaFileId.toLowerCase(Locale.ROOT));
            if (candidate != null) {
                results.add(new ParsedRecommendation(candidate, null));
            }
            if (results.size() >= maxRecommendations) {
                break;
            }
        }

        return results;
    }

    private List<ParsedRecommendation> parseByTitleMentions(String response,
                                                            List<MediaFile> candidates,
                                                            int maxRecommendations) {
        List<ParsedRecommendation> results = new ArrayList<>();
        if (response == null || response.isBlank()) {
            return results;
        }

        String normalizedResponse = normalizeText(response);

        List<MediaFile> sortedCandidates = candidates.stream()
                .filter(candidate -> candidate.getMedia() != null && candidate.getMedia().getTitle() != null)
                .sorted(Comparator.comparingInt((MediaFile mf) -> mf.getMedia().getTitle().length()).reversed())
                .toList();

        for (MediaFile candidate : sortedCandidates) {
            String normalizedTitle = normalizeText(candidate.getMedia().getTitle());
            if (normalizedTitle.length() < 3) {
                continue;
            }

            if (normalizedResponse.contains(normalizedTitle)) {
                results.add(new ParsedRecommendation(candidate, null));
            }

            if (results.size() >= maxRecommendations) {
                break;
            }
        }

        return results;
    }

    private List<ParsedRecommendation> parseJsonRecommendations(String response, Map<String, MediaFile> candidateMap) {
        Optional<JsonNode> jsonNode = tryParseJsonNode(response);
        if (jsonNode.isEmpty()) {
            return List.of();
        }

        JsonNode root = jsonNode.get();
        JsonNode recommendationsNode = root.get("recommendations");
        if (recommendationsNode == null || !recommendationsNode.isArray()) {
            recommendationsNode = root.get("results");
        }
        if (recommendationsNode == null || !recommendationsNode.isArray()) {
            return List.of();
        }

        List<ParsedRecommendation> results = new ArrayList<>();
        for (JsonNode recommendationNode : recommendationsNode) {
            String id = textField(recommendationNode, "id", "mediaFileId", "media_id");
            String reason = textField(recommendationNode, "reason", "why", "explanation");
            addResultIfCandidateExists(results, candidateMap, id, reason);
        }

        return results;
    }

    private Optional<JsonNode> tryParseJsonNode(String response) {
        if (response == null || response.isBlank()) {
            return Optional.empty();
        }

        List<String> candidates = getCandidates(response);

        for (String candidate : candidates) {
            try {
                return Optional.of(objectMapper.readTree(candidate));
            } catch (Exception ignored) {
                // Try next candidate representation
            }
        }

        return Optional.empty();
    }

    private static @NonNull List<String> getCandidates(String response) {
        List<String> candidates = new ArrayList<>();
        candidates.add(response.trim());

        String withoutFence = response
                .replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "")
                .trim();
        candidates.add(withoutFence);

        int firstBrace = response.indexOf('{');
        int lastBrace = response.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            candidates.add(response.substring(firstBrace, lastBrace + 1));
        }
        return candidates;
    }

    private String textField(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode valueNode = node.get(field);
            if (valueNode != null && valueNode.isTextual()) {
                String value = valueNode.asText();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private void addResultIfCandidateExists(List<ParsedRecommendation> results,
                                            Map<String, MediaFile> candidateMap,
                                            String mediaFileId,
                                            String reason) {
        if (mediaFileId == null || mediaFileId.isBlank()) {
            return;
        }

        MediaFile candidate = candidateMap.get(mediaFileId.trim().toLowerCase(Locale.ROOT));
        if (candidate != null) {
            results.add(new ParsedRecommendation(candidate, reason));
        }
    }

    private String normalizeText(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public record ParsedRecommendation(MediaFile mediaFile, String reason) {}
}

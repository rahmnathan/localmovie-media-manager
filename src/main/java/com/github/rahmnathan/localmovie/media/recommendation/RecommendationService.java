package com.github.rahmnathan.localmovie.media.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rahmnathan.localmovie.data.MediaFileType;
import com.github.rahmnathan.localmovie.media.recommendation.ollama.OllamaClient;
import com.github.rahmnathan.localmovie.persistence.entity.*;
import com.github.rahmnathan.localmovie.persistence.entity.Media;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaRecommendationRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaUserRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {
    private static final int MAX_RECOMMENDATIONS = 10;
    private static final int MIN_ACCEPTABLE_RECOMMENDATIONS = 4;
    private static final int MAX_HISTORY_FOR_PROMPT = 12;
    private static final int MAX_CANDIDATES_FOR_PROMPT = 35;
    private static final int MAX_REASON_LENGTH = 180;
    private static final int PROMPT_TOKEN_BUDGET = 3000;
    private static final Pattern SIMPLE_LINE_PATTERN = Pattern.compile(
            "^(?:[-*]|\\d+[.)]|\\[\\d+\\])?\\s*([A-Za-z0-9._:-]{4,})\\s*(?:\\||-|\\u2013|\\u2014|:)\\s*(.+)$");
    private static final Pattern ID_KEY_VALUE_PATTERN = Pattern.compile(
            "(?i)\\b(?:id|mediafileid|media_id)\\s*[:=]\\s*([A-Za-z0-9._:-]{4,})\\b");
    private static final Pattern UUID_ANYWHERE_PATTERN = Pattern.compile(
            "([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})",
            Pattern.CASE_INSENSITIVE);

    private final OllamaClient ollamaClient;
    private final MediaUserRepository userRepository;
    private final MediaViewRepository viewRepository;
    private final MediaFileRepository fileRepository;
    private final MediaRecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public GenerationReport generateRecommendationsForUser(String userId) {
        log.info("Generating recommendations for user: {}", userId);

        if (!ollamaClient.isAvailable()) {
            log.warn("Ollama is not available, skipping recommendation generation");
            return GenerationReport.skipped(userId, "ollama_unavailable");
        }

        Optional<MediaUser> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            log.warn("User {} not found, skipping recommendations", userId);
            return GenerationReport.skipped(userId, "user_not_found");
        }

        MediaUser user = userOpt.get();
        int existingCount = recommendationRepository.findByMediaUserUserIdOrderByRankAsc(userId).size();

        // Get user's watch history (movies/series they've watched)
        List<MediaFile> watchHistory = getWatchHistory(userId);
        if (watchHistory.isEmpty()) {
            log.info("No watch history for user {}, skipping recommendations", userId);
            return GenerationReport.skipped(userId, "no_watch_history");
        }

        // Get IDs of watched content to exclude
        Set<String> watchedIds = watchHistory.stream()
                .map(MediaFile::getMediaFileId)
                .collect(Collectors.toSet());

        // Get candidate movies/series for recommendation (unwatched)
        List<MediaFile> candidates = getCandidateMedia(watchedIds);
        if (candidates.isEmpty()) {
            log.info("No unwatched candidates for user {}", userId);
            return GenerationReport.skipped(userId, "no_candidates");
        }

        // Build prompt and get recommendations from Ollama
        String prompt = buildPrompt(watchHistory, candidates);
        int promptTokens = estimateTokens(prompt);
        log.info("Ollama prompt prepared: chars={}, estimatedTokens={}, historyItems={}, candidates={}",
                prompt.length(), promptTokens, watchHistory.size(), candidates.size());
        String response = ollamaClient.generate(prompt);

        RecommendationSource source = RecommendationSource.OLLAMA;
        List<RecommendationResult> results = response == null || response.isBlank()
                ? List.of()
                : parseResponse(response, candidates);
        int parsedCount = results.size();
        int responseLength = response == null ? 0 : response.length();

        if (isLowQuality(results, candidates.size())) {
            log.info("Ollama recommendations were low quality (count={}), falling back to heuristic ranking for user {}",
                    results.size(), userId);
            results = buildFallbackRecommendations(watchHistory, candidates);
            source = RecommendationSource.HEURISTIC_FALLBACK;
        }

        results = deduplicateAndRank(results);
        boolean finalLowQuality = isLowQuality(results, candidates.size());

        if (results.isEmpty()) {
            if (existingCount > 0) {
                log.warn("No valid recommendations generated for user {}. Keeping {} existing recommendations.", userId, existingCount);
                return logAndReturn(GenerationReport.fromExisting(
                        userId, source, parsedCount, 0, existingCount, watchHistory.size(), candidates.size(), promptTokens, responseLength, true));
            }
            log.warn("No valid recommendations generated for user {} and no existing recommendations available.", userId);
            return logAndReturn(GenerationReport.generated(
                    userId, source, parsedCount, 0, existingCount, watchHistory.size(), candidates.size(), promptTokens, responseLength, false, true));
        }

        if (finalLowQuality && existingCount > 0) {
            log.warn("Generated recommendations still low quality for user {} (newCount={}, existingCount={}), keeping existing.",
                    userId, results.size(), existingCount);
            return logAndReturn(GenerationReport.fromExisting(
                    userId, source, parsedCount, results.size(), existingCount, watchHistory.size(), candidates.size(), promptTokens, responseLength, true));
        }

        saveRecommendations(user, results);

        log.info("Generated {} recommendations for user {}", results.size(), userId);
        return logAndReturn(GenerationReport.generated(
                userId, source, parsedCount, results.size(), existingCount, watchHistory.size(), candidates.size(), promptTokens, responseLength, true, finalLowQuality));
    }

    private GenerationReport logAndReturn(GenerationReport report) {
        log.info("Recommendation generation report: userId={} source={} reason={} persisted={} usedExisting={} " +
                        "parsed={} final={} existing={} history={} candidates={} promptTokens={} responseLength={} lowQuality={}",
                report.userId(),
                report.source(),
                report.reason(),
                report.persisted(),
                report.usedExisting(),
                report.parsedCount(),
                report.finalCount(),
                report.existingCount(),
                report.historyCount(),
                report.candidateCount(),
                report.promptTokens(),
                report.responseLength(),
                report.lowQuality());
        return report;
    }

    private List<MediaFile> getWatchHistory(String userId) {
        // Get recently watched movies and series (not episodes - we want the parent series)
        return viewRepository.findRecentByUserIdWithMedia(userId, LocalDateTime.now().minusMonths(6))
                .stream()
                .map(MediaView::getMediaFile)
                .filter(mf -> mf.getMedia() != null)
                .filter(mf -> mf.getMediaFileType() == MediaFileType.MOVIE ||
                        mf.getMediaFileType() == MediaFileType.EPISODE)
                .limit(MAX_HISTORY_FOR_PROMPT)
                .toList();
    }

    private List<MediaFile> getCandidateMedia(Set<String> excludeIds) {
        // Get movies and series that haven't been watched
        // Use random selection to get diverse candidates rather than always picking highest-rated
        // If excludeIds is empty, use a dummy value to avoid SQL issues
        Set<String> safeExcludeIds = excludeIds.isEmpty() ? Set.of("__none__") : excludeIds;
        return fileRepository.findRandomCandidatesForRecommendation(safeExcludeIds, PageRequest.of(0, MAX_CANDIDATES_FOR_PROMPT));
    }

    private String buildPrompt(List<MediaFile> watchHistory, List<MediaFile> candidates) {
        StringBuilder prompt = new StringBuilder();

        // Extract genres from watch history
        String watchedGenres = extractTopGenres(watchHistory, 5).stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));

        prompt.append("You are a recommendation engine. Choose from catalog IDs only.\n");
        prompt.append("Return ONLY valid JSON. No markdown. No prose.\n");
        prompt.append("JSON schema:\n");
        prompt.append("{\"recommendations\":[{\"id\":\"<catalog_id>\",\"reason\":\"<short reason>\"}]}\n");
        prompt.append("Rules:\n");
        prompt.append("- Recommend up to ").append(MAX_RECOMMENDATIONS).append(" items\n");
        prompt.append("- Use only IDs from CATALOG below\n");
        prompt.append("- Reasons must be <= 140 chars and specific\n");
        prompt.append("- No duplicate IDs\n\n");

        prompt.append("WATCH_HISTORY:\n");
        for (MediaFile mf : watchHistory.stream().limit(MAX_HISTORY_FOR_PROMPT).toList()) {
            Media media = mf.getMedia();
            if (media != null) {
                prompt.append(String.format("- %s (%s) - %s\n",
                        media.getTitle(),
                        media.getReleaseYear() != null ? media.getReleaseYear() : "?",
                        media.getGenre() != null ? media.getGenre() : "Unknown genre"));
            }
        }

        prompt.append("\nTOP_GENRES: ").append(watchedGenres).append("\n\n");

        prompt.append("CATALOG:\n");
        int includedCandidates = 0;
        for (MediaFile mf : candidates) {
            Media media = mf.getMedia();
            if (media != null) {
                String candidateLine = String.format("%s | %s | %s | %s | imdb=%s\n",
                        mf.getMediaFileId(),
                        media.getTitle(),
                        media.getReleaseYear() != null ? media.getReleaseYear() : "?",
                        media.getGenre() != null ? media.getGenre() : "Unknown genre",
                        media.getImdbRating() != null ? media.getImdbRating() : "?");

                if (includedCandidates > 0 && estimateTokens(prompt.toString() + candidateLine) > PROMPT_TOKEN_BUDGET) {
                    break;
                }

                prompt.append(candidateLine);
                includedCandidates++;
            }
        }
        return prompt.toString();
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (text.length() + 3) / 4;
    }

    private List<Map.Entry<String, Long>> extractTopGenres(List<MediaFile> watchHistory, int limit) {
        Map<String, Long> genres = watchHistory.stream()
                .map(MediaFile::getMedia)
                .filter(Objects::nonNull)
                .map(Media::getGenre)
                .filter(Objects::nonNull)
                .flatMap(g -> splitGenres(g).stream())
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()));

        return genres.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .toList();
    }

    private List<RecommendationResult> parseResponse(String response, List<MediaFile> candidates) {
        log.debug("Ollama raw response:\n{}", response);
        Map<String, MediaFile> candidateMap = candidates.stream()
                .filter(mf -> mf.getMediaFileId() != null)
                .collect(Collectors.toMap(mf -> mf.getMediaFileId().toLowerCase(Locale.ROOT), mf -> mf, (a, b) -> a));

        List<RecommendationResult> fromJson = parseJsonRecommendations(response, candidateMap);
        if (!fromJson.isEmpty()) {
            return deduplicateAndRank(fromJson);
        }

        List<RecommendationResult> fromAnyIdMention = parseByIdMentions(response, candidateMap);
        if (!fromAnyIdMention.isEmpty()) {
            return deduplicateAndRank(fromAnyIdMention);
        }

        List<RecommendationResult> fromTitleMentions = parseByTitleMentions(response, candidates);
        if (!fromTitleMentions.isEmpty()) {
            return deduplicateAndRank(fromTitleMentions);
        }

        List<RecommendationResult> results = new ArrayList<>();
        for (String line : response.split("\n")) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

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

        return deduplicateAndRank(results);
    }

    private List<RecommendationResult> parseByIdMentions(String response, Map<String, MediaFile> candidateMap) {
        List<RecommendationResult> results = new ArrayList<>();

        Matcher matcher = UUID_ANYWHERE_PATTERN.matcher(response);
        while (matcher.find()) {
            String mediaFileId = matcher.group(1);
            MediaFile candidate = candidateMap.get(mediaFileId.toLowerCase(Locale.ROOT));
            if (candidate != null) {
                results.add(new RecommendationResult(candidate, buildDefaultReason(candidate), 0));
            }
            if (results.size() >= MAX_RECOMMENDATIONS) {
                break;
            }
        }

        return results;
    }

    private List<RecommendationResult> parseByTitleMentions(String response, List<MediaFile> candidates) {
        List<RecommendationResult> results = new ArrayList<>();
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
                results.add(new RecommendationResult(candidate, buildDefaultReason(candidate), 0));
            }

            if (results.size() >= MAX_RECOMMENDATIONS) {
                break;
            }
        }

        return results;
    }

    private List<RecommendationResult> parseJsonRecommendations(String response, Map<String, MediaFile> candidateMap) {
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

        List<RecommendationResult> results = new ArrayList<>();
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

        for (String candidate : candidates) {
            try {
                return Optional.of(objectMapper.readTree(candidate));
            } catch (Exception ignored) {
                // Try next candidate representation
            }
        }

        return Optional.empty();
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

    private void addResultIfCandidateExists(List<RecommendationResult> results,
                                            Map<String, MediaFile> candidateMap,
                                            String mediaFileId,
                                            String reason) {
        if (mediaFileId == null || mediaFileId.isBlank()) {
            return;
        }

        MediaFile candidate = candidateMap.get(mediaFileId.trim().toLowerCase(Locale.ROOT));
        if (candidate != null) {
            results.add(new RecommendationResult(candidate, sanitizeReason(reason, candidate), 0));
        }
    }

    private List<RecommendationResult> deduplicateAndRank(List<RecommendationResult> rawResults) {
        if (rawResults.isEmpty()) {
            return List.of();
        }

        LinkedHashMap<String, RecommendationResult> unique = new LinkedHashMap<>();
        for (RecommendationResult result : rawResults) {
            String mediaFileId = result.mediaFile().getMediaFileId();
            unique.putIfAbsent(mediaFileId, result);
            if (unique.size() >= MAX_RECOMMENDATIONS) {
                break;
            }
        }

        List<RecommendationResult> ranked = new ArrayList<>();
        int rank = 1;
        for (RecommendationResult result : unique.values()) {
            ranked.add(new RecommendationResult(result.mediaFile(), result.reason(), rank++));
        }
        return ranked;
    }

    private String sanitizeReason(String reason, MediaFile mediaFile) {
        String cleaned = reason == null ? "" : reason.replaceAll("\\s+", " ").trim();
        if (cleaned.isBlank()) {
            return buildDefaultReason(mediaFile);
        }
        if (cleaned.length() > MAX_REASON_LENGTH) {
            return cleaned.substring(0, MAX_REASON_LENGTH - 3) + "...";
        }
        if (cleaned.length() < 12) {
            return buildDefaultReason(mediaFile);
        }
        return cleaned;
    }

    private String buildDefaultReason(MediaFile mediaFile) {
        if (mediaFile == null || mediaFile.getMedia() == null) {
            return "Recommended based on your recent watch history.";
        }

        List<String> genres = splitGenres(mediaFile.getMedia().getGenre());
        if (genres.isEmpty()) {
            return "Recommended based on your recent watch history.";
        }

        return "Matches your interest in " + String.join(", ", genres.stream().limit(2).toList()) + ".";
    }

    private List<RecommendationResult> buildFallbackRecommendations(List<MediaFile> watchHistory, List<MediaFile> candidates) {
        Map<String, Long> preferredGenres = extractTopGenres(watchHistory, 8).stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Comparator<ScoredCandidate> comparator = Comparator
                .comparingDouble(ScoredCandidate::score).reversed()
                .thenComparing(scored -> scored.mediaFile().getMedia().getTitle(), String.CASE_INSENSITIVE_ORDER);

        List<ScoredCandidate> scoredCandidates = candidates.stream()
                .filter(candidate -> candidate.getMedia() != null)
                .collect(Collectors.toMap(
                        MediaFile::getMediaFileId,
                        candidate -> candidate,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ))
                .values().stream()
                .map(candidate -> scoreCandidate(candidate, preferredGenres))
                .sorted(comparator)
                .limit(MAX_RECOMMENDATIONS)
                .toList();

        List<RecommendationResult> results = new ArrayList<>();
        int rank = 1;
        for (ScoredCandidate scoredCandidate : scoredCandidates) {
            String reason = scoredCandidate.matchedGenres().isEmpty()
                    ? "Strong unwatched pick aligned with your recent viewing."
                    : "Because you often watch " + String.join(", ", scoredCandidate.matchedGenres().stream().limit(2).toList()) + ".";
            results.add(new RecommendationResult(scoredCandidate.mediaFile(), sanitizeReason(reason, scoredCandidate.mediaFile()), rank++));
        }

        return results;
    }

    private ScoredCandidate scoreCandidate(MediaFile candidate, Map<String, Long> preferredGenres) {
        List<String> candidateGenres = splitGenres(candidate.getMedia().getGenre());
        List<String> matchedGenres = candidateGenres.stream()
                .filter(preferredGenres::containsKey)
                .toList();

        double genreScore = matchedGenres.stream()
                .mapToDouble(g -> preferredGenres.getOrDefault(g, 0L))
                .sum();

        double imdbScore = parseImdbRating(candidate.getMedia().getImdbRating());
        double totalScore = (genreScore * 10.0) + imdbScore;
        return new ScoredCandidate(candidate, totalScore, matchedGenres);
    }

    private double parseImdbRating(String imdbRating) {
        if (imdbRating == null || imdbRating.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(imdbRating.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private List<String> splitGenres(String genreString) {
        if (genreString == null || genreString.isBlank()) {
            return List.of();
        }

        return Arrays.stream(genreString.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
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

    private boolean isLowQuality(List<RecommendationResult> results, int candidateCount) {
        if (results.isEmpty()) {
            return true;
        }

        int minimumExpected = Math.min(MIN_ACCEPTABLE_RECOMMENDATIONS, candidateCount);
        if (results.size() < minimumExpected) {
            return true;
        }

        long distinctReasons = results.stream().map(RecommendationResult::reason).distinct().count();
        return distinctReasons <= 1;
    }

    @Transactional
    protected void saveRecommendations(MediaUser user, List<RecommendationResult> results) {
        List<RecommendationResult> dedupedResults = deduplicateAndRank(results);

        // Clear existing recommendations for this user
        recommendationRepository.deleteByMediaUserUserId(user.getUserId());
        recommendationRepository.flush();

        // Save new recommendations
        for (RecommendationResult result : dedupedResults) {
            MediaRecommendation recommendation = new MediaRecommendation(
                    result.mediaFile(),
                    user,
                    result.reason(),
                    result.rank()
            );
            recommendationRepository.save(recommendation);
        }
    }

    public List<MediaRecommendation> getRecommendationsForUser(String userId) {
        return recommendationRepository.findByMediaUserUserIdOrderByRankAsc(userId);
    }

    public boolean hasRecommendations(String userId) {
        return recommendationRepository.existsByMediaUserUserId(userId);
    }

    public enum RecommendationSource {
        OLLAMA,
        HEURISTIC_FALLBACK,
        EXISTING_STALE,
        SKIPPED
    }

    public record GenerationReport(
            String userId,
            RecommendationSource source,
            String reason,
            int parsedCount,
            int finalCount,
            int existingCount,
            int historyCount,
            int candidateCount,
            int promptTokens,
            int responseLength,
            boolean persisted,
            boolean usedExisting,
            boolean lowQuality
    ) {
        static GenerationReport skipped(String userId, String reason) {
            return new GenerationReport(userId, RecommendationSource.SKIPPED, reason,
                    0, 0, 0, 0, 0, 0, 0, false, false, false);
        }

        static GenerationReport generated(String userId,
                                          RecommendationSource source,
                                          int parsedCount,
                                          int finalCount,
                                          int existingCount,
                                          int historyCount,
                                          int candidateCount,
                                          int promptTokens,
                                          int responseLength,
                                          boolean persisted,
                                          boolean lowQuality) {
            return new GenerationReport(userId, source, "generated",
                    parsedCount, finalCount, existingCount, historyCount, candidateCount,
                    promptTokens, responseLength, persisted, false, lowQuality);
        }

        static GenerationReport fromExisting(String userId,
                                             RecommendationSource sourceBeforeFallback,
                                             int parsedCount,
                                             int finalCount,
                                             int existingCount,
                                             int historyCount,
                                             int candidateCount,
                                             int promptTokens,
                                             int responseLength,
                                             boolean lowQuality) {
            return new GenerationReport(userId, RecommendationSource.EXISTING_STALE, "kept_existing_after_" + sourceBeforeFallback.name().toLowerCase(Locale.ROOT),
                    parsedCount, finalCount, existingCount, historyCount, candidateCount,
                    promptTokens, responseLength, false, true, lowQuality);
        }
    }

    private record RecommendationResult(MediaFile mediaFile, String reason, int rank) {}

    private record ScoredCandidate(MediaFile mediaFile, double score, List<String> matchedGenres) {}
}

package com.github.rahmnathan.localmovie.media.recommendation;

import com.github.rahmnathan.localmovie.data.MediaFileType;
import com.github.rahmnathan.localmovie.media.recommendation.ollama.OllamaClient;
import com.github.rahmnathan.localmovie.persistence.entity.*;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {
    private static final int MAX_RECOMMENDATIONS = 10;
    private static final int MIN_ACCEPTABLE_RECOMMENDATIONS = 4;
    private static final int MAX_HISTORY_FOR_PROMPT = 12;
    private static final int MAX_CANDIDATES_FOR_PROMPT = 24;
    private static final int MAX_REASON_LENGTH = 180;

    private final OllamaClient ollamaClient;
    private final MediaUserRepository userRepository;
    private final MediaViewRepository viewRepository;
    private final MediaFileRepository fileRepository;
    private final MediaRecommendationRepository recommendationRepository;
    private final RecommendationPromptBuilder promptBuilder;
    private final RecommendationResponseParser responseParser;

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
        String prompt = promptBuilder.buildPrompt(watchHistory, candidates);
        int promptTokens = promptBuilder.estimateTokens(prompt);
        log.info("Ollama prompt prepared: chars={}, estimatedTokens={}, historyItems={}, candidates={}",
                prompt.length(), promptTokens, watchHistory.size(), candidates.size());
        String response = ollamaClient.generate(prompt);

        RecommendationSource source = RecommendationSource.OLLAMA;
        List<RecommendationResult> results = response == null || response.isBlank()
                ? List.of()
                : toRecommendationResults(responseParser.parseResponse(response, candidates, MAX_RECOMMENDATIONS));
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
        // Get movies and series that haven't been watched.
        // We avoid ORDER BY RANDOM() because it full-sorts candidates and becomes expensive as the library grows.
        // Instead, pick a random page over a deterministic indexed order.
        Set<String> safeExcludeIds = excludeIds.isEmpty() ? Set.of("__none__") : excludeIds;
        long candidateCount = fileRepository.countCandidatesForRecommendation(safeExcludeIds);
        if (candidateCount <= 0) {
            return List.of();
        }

        int totalPages = (int) Math.max(1, Math.ceil((double) candidateCount / MAX_CANDIDATES_FOR_PROMPT));
        int page = ThreadLocalRandom.current().nextInt(totalPages);

        return fileRepository.findCandidatesForRecommendationPage(
                safeExcludeIds,
                PageRequest.of(page, MAX_CANDIDATES_FOR_PROMPT)
        );
    }

    private List<RecommendationResult> toRecommendationResults(List<RecommendationResponseParser.ParsedRecommendation> parsed) {
        if (parsed == null || parsed.isEmpty()) {
            return List.of();
        }
        return parsed.stream()
                .map(item -> new RecommendationResult(
                        item.mediaFile(),
                        sanitizeReason(item.reason(), item.mediaFile()),
                        0))
                .toList();
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

        List<String> genres = promptBuilder.splitGenres(mediaFile.getMedia().getGenre());
        if (genres.isEmpty()) {
            return "Recommended based on your recent watch history.";
        }

        return "Matches your interest in " + String.join(", ", genres.stream().limit(2).toList()) + ".";
    }

    private List<RecommendationResult> buildFallbackRecommendations(List<MediaFile> watchHistory, List<MediaFile> candidates) {
        Map<String, Long> preferredGenres = promptBuilder.extractTopGenres(watchHistory, 8).stream()
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
        List<String> candidateGenres = promptBuilder.splitGenres(candidate.getMedia().getGenre());
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

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {
    private static final int MAX_RECOMMENDATIONS = 10;
    private static final int MAX_HISTORY_FOR_PROMPT = 20;
    private static final int MAX_CANDIDATES_FOR_PROMPT = 50;

    private final OllamaClient ollamaClient;
    private final MediaUserRepository userRepository;
    private final MediaViewRepository viewRepository;
    private final MediaFileRepository fileRepository;
    private final MediaRecommendationRepository recommendationRepository;

    @Transactional
    public void generateRecommendationsForUser(String userId) {
        log.info("Generating recommendations for user: {}", userId);

        if (!ollamaClient.isAvailable()) {
            log.warn("Ollama is not available, skipping recommendation generation");
            return;
        }

        Optional<MediaUser> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            log.warn("User {} not found, skipping recommendations", userId);
            return;
        }

        MediaUser user = userOpt.get();

        // Get user's watch history (movies/series they've watched)
        List<MediaFile> watchHistory = getWatchHistory(userId);
        if (watchHistory.isEmpty()) {
            log.info("No watch history for user {}, skipping recommendations", userId);
            return;
        }

        // Get IDs of watched content to exclude
        Set<String> watchedIds = watchHistory.stream()
                .map(MediaFile::getMediaFileId)
                .collect(Collectors.toSet());

        // Get candidate movies/series for recommendation (unwatched)
        List<MediaFile> candidates = getCandidateMedia(watchedIds);
        if (candidates.isEmpty()) {
            log.info("No unwatched candidates for user {}", userId);
            return;
        }

        // Build prompt and get recommendations from Ollama
        String prompt = buildPrompt(watchHistory, candidates);
        String response = ollamaClient.generate(prompt);

        if (response == null || response.isBlank()) {
            log.info("Empty response from Ollama for user {}", userId);
            return;
        }

        // Parse response and save recommendations
        List<RecommendationResult> results = parseResponse(response, candidates);
        saveRecommendations(user, results);

        log.info("Generated {} recommendations for user {}", results.size(), userId);
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
        // If excludeIds is empty, use a dummy value to avoid SQL issues
        Set<String> safeExcludeIds = excludeIds.isEmpty() ? Set.of("__none__") : excludeIds;
        return fileRepository.findCandidatesForRecommendation(safeExcludeIds, PageRequest.of(0, MAX_CANDIDATES_FOR_PROMPT));
    }

    private String buildPrompt(List<MediaFile> watchHistory, List<MediaFile> candidates) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a movie recommendation system. Based on the user's watch history, ");
        prompt.append("recommend movies or series from the available library.\n\n");

        prompt.append("USER'S WATCH HISTORY:\n");
        for (MediaFile mf : watchHistory) {
            Media media = mf.getMedia();
            if (media != null) {
                prompt.append(String.format("- %s (%s) - %s - Rating: %s\n",
                        media.getTitle(),
                        media.getReleaseYear() != null ? media.getReleaseYear() : "Unknown year",
                        media.getGenre() != null ? media.getGenre() : "Unknown genre",
                        media.getImdbRating() != null ? media.getImdbRating() : "N/A"));
            }
        }

        prompt.append("\nAVAILABLE TITLES TO RECOMMEND FROM (use exact titles):\n");
        for (MediaFile mf : candidates) {
            Media media = mf.getMedia();
            if (media != null) {
                prompt.append(String.format("- ID:%s | %s (%s) - %s - %s\n",
                        mf.getMediaFileId(),
                        media.getTitle(),
                        media.getReleaseYear() != null ? media.getReleaseYear() : "Unknown",
                        media.getGenre() != null ? media.getGenre() : "Unknown genre",
                        media.getPlot() != null ? truncate(media.getPlot(), 100) : "No description"));
            }
        }

        prompt.append("\nProvide your top ").append(MAX_RECOMMENDATIONS).append(" recommendations ");
        prompt.append("from the available titles above. For each recommendation, respond in this exact format:\n");
        prompt.append("RECOMMENDATION: ID:<media_file_id> | REASON: <brief reason why they'd like it>\n\n");
        prompt.append("Only recommend titles from the available list. Be concise with reasons (under 100 chars).");

        return prompt.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private List<RecommendationResult> parseResponse(String response, List<MediaFile> candidates) {
        List<RecommendationResult> results = new ArrayList<>();

        // Create lookup map for candidates
        Map<String, MediaFile> candidateMap = candidates.stream()
                .collect(Collectors.toMap(MediaFile::getMediaFileId, mf -> mf, (a, b) -> a));

        // Parse lines looking for RECOMMENDATION: ID:xxx | REASON: yyy
        Pattern pattern = Pattern.compile("RECOMMENDATION:\\s*ID:([^|]+)\\|\\s*REASON:\\s*(.+)", Pattern.CASE_INSENSITIVE);

        int rank = 1;
        for (String line : response.split("\n")) {
            Matcher matcher = pattern.matcher(line.trim());
            if (matcher.find()) {
                String mediaFileId = matcher.group(1).trim();
                String reason = matcher.group(2).trim();

                MediaFile candidate = candidateMap.get(mediaFileId);
                if (candidate != null) {
                    results.add(new RecommendationResult(candidate, reason, rank++));
                    if (results.size() >= MAX_RECOMMENDATIONS) break;
                }
            }
        }

        return results;
    }

    @Transactional
    protected void saveRecommendations(MediaUser user, List<RecommendationResult> results) {
        // Clear existing recommendations for this user
        recommendationRepository.deleteByMediaUserUserId(user.getUserId());

        // Save new recommendations
        for (RecommendationResult result : results) {
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

    private record RecommendationResult(MediaFile mediaFile, String reason, int rank) {}
}

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
import com.github.rahmnathan.localmovie.persistence.entity.Media;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {
    private static final int MAX_RECOMMENDATIONS = 10;
    private static final int MAX_HISTORY_FOR_PROMPT = 20;
    private static final int MAX_CANDIDATES_FOR_PROMPT = 100;

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
        // Use random selection to get diverse candidates rather than always picking highest-rated
        // If excludeIds is empty, use a dummy value to avoid SQL issues
        Set<String> safeExcludeIds = excludeIds.isEmpty() ? Set.of("__none__") : excludeIds;
        return fileRepository.findRandomCandidatesForRecommendation(safeExcludeIds, PageRequest.of(0, MAX_CANDIDATES_FOR_PROMPT));
    }

    private String buildPrompt(List<MediaFile> watchHistory, List<MediaFile> candidates) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert movie recommendation system. Analyze the user's watch history carefully ");
        prompt.append("to understand their preferences (genres, themes, era, mood, style) and recommend titles they would genuinely enjoy.\n\n");

        prompt.append("USER'S RECENT WATCH HISTORY (most recent first):\n");
        for (MediaFile mf : watchHistory) {
            Media media = mf.getMedia();
            if (media != null) {
                prompt.append(String.format("- %s (%s) - Genre: %s - Rating: %s\n",
                        media.getTitle(),
                        media.getReleaseYear() != null ? media.getReleaseYear() : "Unknown year",
                        media.getGenre() != null ? media.getGenre() : "Unknown genre",
                        media.getImdbRating() != null ? media.getImdbRating() : "N/A"));
            }
        }

        // Extract genres from watch history to highlight patterns
        String watchedGenres = watchHistory.stream()
                .map(MediaFile::getMedia)
                .filter(Objects::nonNull)
                .map(Media::getGenre)
                .filter(Objects::nonNull)
                .flatMap(g -> Arrays.stream(g.split(",\\s*")))
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));

        if (!watchedGenres.isEmpty()) {
            prompt.append("\nOBSERVED PREFERENCES: User frequently watches: ").append(watchedGenres).append("\n");
        }

        prompt.append("\nAVAILABLE TITLES TO RECOMMEND FROM:\n");
        for (MediaFile mf : candidates) {
            Media media = mf.getMedia();
            if (media != null) {
                prompt.append(String.format("- ID:%s | %s (%s) - %s - %s\n",
                        mf.getMediaFileId(),
                        media.getTitle(),
                        media.getReleaseYear() != null ? media.getReleaseYear() : "Unknown",
                        media.getGenre() != null ? media.getGenre() : "Unknown genre",
                        media.getPlot() != null ? truncate(media.getPlot(), 150) : "No description"));
            }
        }

        prompt.append("\nINSTRUCTIONS:\n");
        prompt.append("1. Match recommendations to the user's demonstrated genre preferences\n");
        prompt.append("2. Consider similar themes, tone, and style - not just genre\n");
        prompt.append("3. Prioritize quality matches over popularity\n");
        prompt.append("4. Include some variety while staying relevant to their taste\n\n");

        prompt.append("Provide your top ").append(MAX_RECOMMENDATIONS).append(" recommendations ");
        prompt.append("from the available titles above. For each recommendation, respond in this exact format:\n");
        prompt.append("RECOMMENDATION: ID:<media_file_id> | REASON: <brief reason connecting to their watch history>\n\n");
        prompt.append("Only recommend titles from the available list. Be specific about why each matches their taste.");

        return prompt.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private List<RecommendationResult> parseResponse(String response, List<MediaFile> candidates) {
        List<RecommendationResult> results = new ArrayList<>();

        log.debug("Ollama raw response:\n{}", response);

        // Create lookup map for candidates
        Map<String, MediaFile> candidateMap = candidates.stream()
                .collect(Collectors.toMap(MediaFile::getMediaFileId, mf -> mf, (a, b) -> a));

        // Try multiple patterns to handle variations in model output
        List<Pattern> patterns = List.of(
                // Standard format: RECOMMENDATION: ID:xxx | REASON: yyy
                Pattern.compile("RECOMMENDATION:\\s*ID:([^|]+)\\|\\s*REASON:\\s*(.+)", Pattern.CASE_INSENSITIVE),
                // Without RECOMMENDATION prefix: ID:xxx | REASON: yyy
                Pattern.compile("^\\d*\\.?\\s*ID:([^|]+)\\|\\s*REASON:\\s*(.+)", Pattern.CASE_INSENSITIVE),
                // Numbered list: 1. ID:xxx | reason or 1. ID:xxx - reason
                Pattern.compile("^\\d+\\.\\s*ID:([^|\\-]+)[|\\-]\\s*(.+)", Pattern.CASE_INSENSITIVE),
                // Just ID somewhere in line: ...ID:xxx...
                Pattern.compile("ID:([a-f0-9\\-]{36})", Pattern.CASE_INSENSITIVE)
        );

        int rank = 1;
        for (String line : response.split("\n")) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(trimmedLine);
                if (matcher.find()) {
                    String mediaFileId = matcher.group(1).trim();
                    String reason = matcher.groupCount() >= 2 ? matcher.group(2).trim() : "Recommended based on your watch history";

                    MediaFile candidate = candidateMap.get(mediaFileId);
                    if (candidate != null) {
                        log.debug("Parsed recommendation: {} -> {}", mediaFileId, candidate.getMedia().getTitle());
                        results.add(new RecommendationResult(candidate, reason, rank++));
                        break; // Found a match for this line, move to next line
                    } else {
                        log.debug("ID not found in candidates: {}", mediaFileId);
                    }
                }
            }

            if (results.size() >= MAX_RECOMMENDATIONS) break;
        }

        if (results.isEmpty()) {
            log.warn("Failed to parse any recommendations from Ollama response. First 500 chars: {}",
                    response.substring(0, Math.min(500, response.length())));
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

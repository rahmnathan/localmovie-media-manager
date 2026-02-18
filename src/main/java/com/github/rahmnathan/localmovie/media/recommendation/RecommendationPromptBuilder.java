package com.github.rahmnathan.localmovie.media.recommendation;

import com.github.rahmnathan.localmovie.persistence.entity.Media;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class RecommendationPromptBuilder {
    private static final int MAX_RECOMMENDATIONS = 10;
    private static final int MAX_HISTORY_FOR_PROMPT = 12;
    private static final int PROMPT_TOKEN_BUDGET = 3000;

    public String buildPrompt(List<MediaFile> watchHistory, List<MediaFile> candidates) {
        StringBuilder prompt = new StringBuilder();

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
            if (media == null) {
                continue;
            }

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

        return prompt.toString();
    }

    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (text.length() + 3) / 4;
    }

    public List<Map.Entry<String, Long>> extractTopGenres(List<MediaFile> watchHistory, int limit) {
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

    public List<String> splitGenres(String genreString) {
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
}

package com.github.rahmnathan.localmovie.media.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rahmnathan.localmovie.data.MediaFileType;
import com.github.rahmnathan.localmovie.media.omdb.MediaType;
import com.github.rahmnathan.localmovie.media.recommendation.ollama.OllamaClient;
import com.github.rahmnathan.localmovie.persistence.entity.Media;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaUser;
import com.github.rahmnathan.localmovie.persistence.entity.MediaView;
import com.github.rahmnathan.localmovie.persistence.repository.MediaFileRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaRecommendationRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaUserRepository;
import com.github.rahmnathan.localmovie.persistence.repository.MediaViewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private OllamaClient ollamaClient;
    @Mock
    private MediaUserRepository userRepository;
    @Mock
    private MediaViewRepository viewRepository;
    @Mock
    private MediaFileRepository fileRepository;
    @Mock
    private MediaRecommendationRepository recommendationRepository;

    private RecommendationService recommendationService;

    private static final String USER_ID = "user-1";
    private MediaUser mediaUser;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(
                ollamaClient,
                userRepository,
                viewRepository,
                fileRepository,
                recommendationRepository,
                new RecommendationPromptBuilder(),
                new RecommendationResponseParser(new ObjectMapper())
        );

        mediaUser = new MediaUser(USER_ID);
        when(ollamaClient.isAvailable()).thenReturn(true);
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(mediaUser));
        when(recommendationRepository.findByMediaUserUserIdOrderByRankAsc(USER_ID)).thenReturn(List.of());
    }

    @Test
    void generateRecommendations_parsesJsonResponseAndSavesRankedResults() {
        List<MediaFile> history = List.of(
                mediaFile("watch-1", "Dune", "Action, Sci-Fi", "8.2")
        );
        when(viewRepository.findRecentByUserIdWithMedia(eq(USER_ID), any(LocalDateTime.class)))
                .thenReturn(List.of(new MediaView(history.get(0), mediaUser, 1200.0, 9000.0)));

        MediaFile c1 = mediaFile("c1", "Arrival", "Sci-Fi, Drama", "7.9");
        MediaFile c2 = mediaFile("c2", "Top Gun: Maverick", "Action, Drama", "8.3");
        MediaFile c3 = mediaFile("c3", "The Matrix", "Action, Sci-Fi", "8.7");
        MediaFile c4 = mediaFile("c4", "Blade Runner 2049", "Sci-Fi, Thriller", "8.0");
        when(fileRepository.findRandomCandidatesForRecommendation(anySet(), any()))
                .thenReturn(List.of(c1, c2, c3, c4));

        when(ollamaClient.generate(anyString())).thenReturn("""
                {"recommendations":[
                  {"id":"c3","reason":"Sci-fi action tone very close to your recent picks."},
                  {"id":"c2","reason":"High-energy action with strong critical reception."},
                  {"id":"c4","reason":"Cinematic sci-fi mood similar to your history."},
                  {"id":"c1","reason":"Thoughtful sci-fi drama aligned with your taste."}
                ]}
                """);

        recommendationService.generateRecommendationsForUser(USER_ID);

        verify(recommendationRepository).deleteByMediaUserUserId(USER_ID);
        ArgumentCaptor<com.github.rahmnathan.localmovie.persistence.entity.MediaRecommendation> captor =
                ArgumentCaptor.forClass(com.github.rahmnathan.localmovie.persistence.entity.MediaRecommendation.class);
        verify(recommendationRepository, times(4)).save(captor.capture());

        List<com.github.rahmnathan.localmovie.persistence.entity.MediaRecommendation> saved = captor.getAllValues();
        assertEquals("c3", saved.get(0).getMediaFile().getMediaFileId());
        assertEquals(1, saved.get(0).getRank());
        assertEquals("c2", saved.get(1).getMediaFile().getMediaFileId());
        assertEquals(2, saved.get(1).getRank());
    }

    @Test
    void generateRecommendations_fallsBackWhenResponseUnparseable() {
        List<MediaFile> history = List.of(
                mediaFile("watch-1", "Mad Max: Fury Road", "Action, Adventure", "8.1")
        );
        when(viewRepository.findRecentByUserIdWithMedia(eq(USER_ID), any(LocalDateTime.class)))
                .thenReturn(List.of(new MediaView(history.get(0), mediaUser, 1200.0, 9000.0)));

        MediaFile c1 = mediaFile("x1", "John Wick", "Action, Thriller", "7.4");
        MediaFile c2 = mediaFile("x2", "The Martian", "Adventure, Sci-Fi", "8.0");
        MediaFile c3 = mediaFile("x3", "Heat", "Action, Crime", "8.3");
        MediaFile c4 = mediaFile("x4", "Interstellar", "Sci-Fi, Drama", "8.6");
        MediaFile c5 = mediaFile("x5", "Die Hard", "Action, Thriller", "8.2");
        when(fileRepository.findRandomCandidatesForRecommendation(anySet(), any()))
                .thenReturn(List.of(c1, c2, c3, c4, c5));

        when(ollamaClient.generate(anyString())).thenReturn("totally malformed response without ids");

        recommendationService.generateRecommendationsForUser(USER_ID);

        verify(recommendationRepository).deleteByMediaUserUserId(USER_ID);
        verify(recommendationRepository, atLeast(1)).save(any());
    }

    @Test
    void generateRecommendations_doesNotDeleteExistingWhenNoValidNewResults() {
        MediaFile watched = mediaFile("watch-1", "The Dark Knight", "Action, Crime", "9.0");
        when(viewRepository.findRecentByUserIdWithMedia(eq(USER_ID), any(LocalDateTime.class)))
                .thenReturn(List.of(new MediaView(watched, mediaUser, 2000.0, 9000.0)));

        MediaFile invalidCandidate = MediaFile.builder()
                .mediaFileId("bad-1")
                .mediaFileType(MediaFileType.MOVIE)
                .media(null)
                .build();
        when(fileRepository.findRandomCandidatesForRecommendation(anySet(), any()))
                .thenReturn(List.of(invalidCandidate));

        when(ollamaClient.generate(anyString())).thenReturn("not parseable");

        recommendationService.generateRecommendationsForUser(USER_ID);

        verify(recommendationRepository, never()).deleteByMediaUserUserId(anyString());
        verify(recommendationRepository, never()).save(any());
    }

    private MediaFile mediaFile(String id, String title, String genre, String imdbRating) {
        Media media = Media.builder()
                .title(title)
                .genre(genre)
                .imdbRating(imdbRating)
                .mediaType(MediaType.MOVIE)
                .build();

        return MediaFile.builder()
                .mediaFileId(id)
                .mediaFileType(MediaFileType.MOVIE)
                .media(media)
                .streamable(true)
                .mediaViews(Set.of())
                .build();
    }
}

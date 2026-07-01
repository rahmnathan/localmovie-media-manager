package com.github.rahmnathan.localmovie.web.admin;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.media.MediaUpdateService;
import com.github.rahmnathan.localmovie.media.recommendation.RecommendationJobService;
import com.github.rahmnathan.localmovie.media.subtitle.SubtitleJobService;
import com.github.rahmnathan.localmovie.persistence.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.Media;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaAdminResourceTest {

    @Mock
    private MediaUpdateService updateService;
    @Mock
    private ServiceConfig serviceConfig;
    @Mock
    private RecommendationJobService recommendationJobService;
    @Mock
    private MediaPersistenceService persistenceService;
    @Mock
    private SubtitleJobService subtitleJobService;

    @Test
    void syncSubtitlesQueuesForcedSubtitleJob() {
        MediaFile mediaFile = MediaFile.builder()
                .id(1L)
                .mediaFileId("media-123")
                .streamable(true)
                .media(Media.builder().imdbId("tt1234567").build())
                .build();
        when(persistenceService.findByMediaFileId("media-123")).thenReturn(Optional.of(mediaFile));
        when(subtitleJobService.queueSubtitleFetch(mediaFile, "tt1234567", true)).thenReturn(true);

        var response = resource().syncSubtitles("media-123");

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(subtitleJobService).queueSubtitleFetch(mediaFile, "tt1234567", true);
    }

    @Test
    void syncSubtitlesReturnsNotFoundForUnknownMedia() {
        when(persistenceService.findByMediaFileId("missing")).thenReturn(Optional.empty());

        var response = resource().syncSubtitles("missing");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private MediaAdminResource resource() {
        return new MediaAdminResource(
                updateService,
                serviceConfig,
                recommendationJobService,
                persistenceService,
                subtitleJobService);
    }
}

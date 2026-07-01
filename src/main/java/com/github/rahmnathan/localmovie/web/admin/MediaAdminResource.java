package com.github.rahmnathan.localmovie.web.admin;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.media.MediaUpdateService;
import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.media.recommendation.RecommendationJobService;
import com.github.rahmnathan.localmovie.media.subtitle.SubtitleJobService;
import com.github.rahmnathan.localmovie.persistence.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(value = "/admin/v1")
public class MediaAdminResource {
    private final MediaUpdateService updateService;
    private final ServiceConfig serviceConfig;
    private final RecommendationJobService recommendationJobService;
    private final MediaPersistenceService persistenceService;
    private final SubtitleJobService subtitleJobService;

    @PostMapping(path = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateMedia(@RequestBody MediaRequest mediaRequest) throws InvalidMediaException {
        log.info("Received updateMedia request: {}", mediaRequest.toString());

        updateService.updateMedia(mediaRequest.getPath());

        log.info("Media updated successfully.");
    }

    @PutMapping(path = "/config/conversion-service")
    public void toggleConversionService(@RequestParam boolean enabled) {
        log.info("Setting conversion service enabled to: {}", enabled);
        serviceConfig.getConversionService().setEnabled(enabled);
    }

    @PostMapping(path = "/recommendations/refresh")
    public ResponseEntity<String> refreshRecommendations() {
        log.info("Manually triggering recommendations refresh");

        if (serviceConfig.getOllama() == null || !serviceConfig.getOllama().isEnabled()) {
            return ResponseEntity.badRequest().body("Ollama is not enabled");
        }

        recommendationJobService.refreshRecommendations();
        return ResponseEntity.ok("Recommendations refresh triggered");
    }

    @Operation(
            summary = "Queue subtitle sync",
            description = "Manually queues a subtitle fetch/sync job for the given media file. Existing English subtitles are replaced when the new job completes.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Subtitle sync job queued"),
            @ApiResponse(responseCode = "400", description = "Media is not streamable or has no IMDb ID"),
            @ApiResponse(responseCode = "404", description = "Media file not found"),
            @ApiResponse(responseCode = "409", description = "Subtitle sync could not be queued because OpenSubtitles is disabled or a job is already active")
    })
    @PostMapping(path = "/media/{mediaFileId}/subtitles/sync")
    public ResponseEntity<Void> syncSubtitles(
            @Parameter(description = "Media file UUID", required = true, example = "f168fb4a-4ee8-43ab-a323-8395c35c31bf")
            @PathVariable("mediaFileId") String mediaFileId) {
        log.info("Manually queueing subtitle sync - {}", mediaFileId);

        Optional<MediaFile> mediaFile = persistenceService.findByMediaFileId(mediaFileId);
        if (mediaFile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (!Boolean.TRUE.equals(mediaFile.get().getStreamable()) || mediaFile.get().getMedia() == null) {
            return ResponseEntity.badRequest().build();
        }

        String imdbId = mediaFile.get().getMedia().getImdbId();
        if (imdbId == null || imdbId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        boolean queued = subtitleJobService.queueSubtitleFetch(mediaFile.get(), imdbId, true);
        if (!queued) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        return ResponseEntity.accepted().build();
    }
}

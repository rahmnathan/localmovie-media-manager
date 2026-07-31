package com.github.rahmnathan.localmovie.web.admin;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.data.MediaJobStatus;
import com.github.rahmnathan.localmovie.data.MediaPath;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.media.MediaUpdateService;
import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;
import com.github.rahmnathan.localmovie.media.recommendation.RecommendationJobService;
import com.github.rahmnathan.localmovie.media.subtitle.SubtitleJobService;
import com.github.rahmnathan.localmovie.persistence.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.Media;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaImage;
import com.github.rahmnathan.localmovie.persistence.entity.MediaJob;
import com.github.rahmnathan.localmovie.persistence.repository.MediaJobRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(value = "/admin/v1")
public class MediaAdminResource {
    private static final Set<String> ACTIVE_STATUSES = Set.of(
            MediaJobStatus.QUEUED.name(),
            MediaJobStatus.RUNNING.name()
    );
    private static final int MAX_POSTER_WIDTH = 500;
    private static final int MAX_POSTER_HEIGHT = 750;

    private final MediaUpdateService updateService;
    private final ServiceConfig serviceConfig;
    private final RecommendationJobService recommendationJobService;
    private final MediaPersistenceService persistenceService;
    private final SubtitleJobService subtitleJobService;
    private final MediaJobRepository mediaJobRepository;

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

    @Operation(
            summary = "Refresh metadata",
            description = "Refreshes metadata for the given media file from external sources (OMDB/TMDB).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Metadata refresh triggered"),
            @ApiResponse(responseCode = "404", description = "Media file not found")
    })
    @PostMapping(path = "/media/{mediaFileId}/metadata/refresh")
    public ResponseEntity<Void> refreshMetadata(
            @Parameter(description = "Media file UUID", required = true, example = "f168fb4a-4ee8-43ab-a323-8395c35c31bf")
            @PathVariable("mediaFileId") String mediaFileId) {
        log.info("Manually refreshing metadata - {}", mediaFileId);

        Optional<MediaFile> mediaFile = persistenceService.findByMediaFileId(mediaFileId);
        if (mediaFile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            updateService.updateMedia(mediaFile.get().getPath());
            return ResponseEntity.accepted().build();
        } catch (InvalidMediaException e) {
            log.error("Failed to refresh metadata for {}", mediaFileId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
            summary = "Queue reconversion",
            description = "Queues a video reconversion job for the given media file. The file will be re-encoded using the standard conversion settings.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Reconversion job queued"),
            @ApiResponse(responseCode = "400", description = "Media is not streamable or path is invalid"),
            @ApiResponse(responseCode = "404", description = "Media file not found"),
            @ApiResponse(responseCode = "409", description = "A conversion job is already active for this file")
    })
    @PostMapping(path = "/media/{mediaFileId}/reconvert")
    public ResponseEntity<Void> queueReconversion(
            @Parameter(description = "Media file UUID", required = true, example = "f168fb4a-4ee8-43ab-a323-8395c35c31bf")
            @PathVariable("mediaFileId") String mediaFileId) {
        log.info("Manually queueing reconversion - {}", mediaFileId);

        Optional<MediaFile> mediaFileOpt = persistenceService.findByMediaFileId(mediaFileId);
        if (mediaFileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MediaFile mediaFile = mediaFileOpt.get();
        if (!Boolean.TRUE.equals(mediaFile.getStreamable())) {
            return ResponseEntity.badRequest().build();
        }

        String absolutePath = mediaFile.getAbsolutePath();
        if (absolutePath == null || absolutePath.isBlank()) {
            log.warn("Media file {} has no absolute path", mediaFileId);
            return ResponseEntity.badRequest().build();
        }

        // Check if a conversion is already active for this file
        if (mediaJobRepository.existsByInputFileAndStatusIn(absolutePath, ACTIVE_STATUSES) ||
                mediaJobRepository.existsByOutputFileAndStatusIn(absolutePath, ACTIVE_STATUSES)) {
            log.warn("Conversion already active for {}", mediaFileId);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        try {
            MediaPath mediaPath = MediaPath.parse(absolutePath);
            String jobId = mediaPath.asJobId();
            String destinationPath = mediaPath.getDestinationPath();

            if (destinationPath == null) {
                log.warn("Could not determine destination path for {}", absolutePath);
                return ResponseEntity.badRequest().build();
            }

            MediaJob mediaJob = MediaJob.builder()
                    .inputFile(absolutePath)
                    .outputFile(destinationPath)
                    .jobId(jobId)
                    .status(MediaJobStatus.QUEUED.name())
                    .build();

            mediaJobRepository.save(mediaJob);
            log.info("Queued reconversion job {} for {}", jobId, mediaFileId);

            return ResponseEntity.accepted().build();
        } catch (InvalidMediaException e) {
            log.error("Failed to parse media path for reconversion: {}", absolutePath, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
            summary = "Update metadata",
            description = "Manually updates metadata fields for the given media file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metadata updated successfully"),
            @ApiResponse(responseCode = "404", description = "Media file not found")
    })
    @PatchMapping(path = "/media/{mediaFileId}/metadata", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateMetadata(
            @Parameter(description = "Media file UUID", required = true, example = "f168fb4a-4ee8-43ab-a323-8395c35c31bf")
            @PathVariable("mediaFileId") String mediaFileId,
            @RequestBody MetadataUpdateRequest request) {
        log.info("Updating metadata for {} - {}", mediaFileId, request);

        Optional<MediaFile> mediaFileOpt = persistenceService.findByMediaFileId(mediaFileId);
        if (mediaFileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MediaFile mediaFile = mediaFileOpt.get();
        Media media = mediaFile.getMedia();
        if (media == null) {
            return ResponseEntity.notFound().build();
        }

        // Update only non-null fields from the request
        if (request.getTitle() != null) {
            media.setTitle(request.getTitle());
        }
        if (request.getReleaseYear() != null) {
            media.setReleaseYear(request.getReleaseYear());
        }
        if (request.getGenre() != null) {
            media.setGenre(request.getGenre());
        }
        if (request.getPlot() != null) {
            media.setPlot(request.getPlot());
        }
        if (request.getActors() != null) {
            media.setActors(request.getActors());
        }
        if (request.getImdbRating() != null) {
            media.setImdbRating(request.getImdbRating());
        }
        if (request.getMetaRating() != null) {
            media.setMetaRating(request.getMetaRating());
        }

        persistenceService.saveMediaFile(mediaFile);
        log.info("Metadata updated for {}", mediaFileId);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Upload poster",
            description = "Uploads a new poster image for the given media file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Poster uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid image file"),
            @ApiResponse(responseCode = "404", description = "Media file not found")
    })
    @PostMapping(path = "/media/{mediaFileId}/poster", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadPoster(
            @Parameter(description = "Media file UUID", required = true, example = "f168fb4a-4ee8-43ab-a323-8395c35c31bf")
            @PathVariable("mediaFileId") String mediaFileId,
            @RequestParam("file") MultipartFile file) {
        log.info("Uploading poster for {} - size: {} bytes", mediaFileId, file.getSize());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Optional<MediaFile> mediaFileOpt = persistenceService.findByMediaFileId(mediaFileId);
        if (mediaFileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MediaFile mediaFile = mediaFileOpt.get();
        Media media = mediaFile.getMedia();
        if (media == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] imageBytes = resizeImage(file.getBytes());
            log.info("Resized poster for {} - final size: {} bytes", mediaFileId, imageBytes.length);

            MediaImage existingImage = media.getImage();
            if (existingImage != null) {
                existingImage.setImage(imageBytes);
            } else {
                MediaImage newImage = new MediaImage(imageBytes, media);
                media.setImage(newImage);
            }

            persistenceService.saveMediaFile(mediaFile);
            log.info("Poster uploaded for {}", mediaFileId);

            return ResponseEntity.ok().build();
        } catch (IOException e) {
            log.error("Failed to process poster file for {}", mediaFileId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    private byte[] resizeImage(byte[] originalBytes) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalBytes));
        if (original == null) {
            throw new IOException("Unable to read image");
        }

        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // If already small enough, return original
        if (originalWidth <= MAX_POSTER_WIDTH && originalHeight <= MAX_POSTER_HEIGHT) {
            return originalBytes;
        }

        // Calculate new dimensions maintaining aspect ratio
        double widthRatio = (double) MAX_POSTER_WIDTH / originalWidth;
        double heightRatio = (double) MAX_POSTER_HEIGHT / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);

        // Create resized image
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();

        // Write to JPEG with good quality
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(resized, "jpg", output);
        return output.toByteArray();
    }

    @Data
    public static class MetadataUpdateRequest {
        private String title;
        private String releaseYear;
        private String genre;
        private String plot;
        private String actors;
        private String imdbRating;
        private String metaRating;
    }
}

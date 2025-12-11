package com.github.rahmnathan.localmovie.media.ffprobe;

import com.github.rahmnathan.localmovie.persistence.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@AllArgsConstructor
public class VideoMetadataService {
    private final FFprobeService ffprobeService;
    private final MediaPersistenceService persistenceService;

    public void analyzeAndUpdate(MediaFile mediaFile) {
        if (mediaFile.getMetadataAnalyzed() != null && mediaFile.getMetadataAnalyzed()) {
            log.debug("Metadata already analyzed for: {}", mediaFile.getPath());
            return;
        }

        try {
            log.info("Analyzing video metadata for: {}", mediaFile.getPath());
            VideoMetadata metadata = ffprobeService.analyzeVideo(mediaFile.getAbsolutePath());

            mediaFile.setVideoCodec(metadata.getVideoCodec());
            mediaFile.setAudioCodec(metadata.getAudioCodec());
            mediaFile.setContainerFormat(metadata.getContainerFormat());
            mediaFile.setDurationSeconds(metadata.getDurationSeconds());
            mediaFile.setBitrateKbps(metadata.getBitrateKbps());
            mediaFile.setResolution(metadata.getResolution());
            mediaFile.setMetadataAnalyzed(true);

            persistenceService.saveMediaFile(mediaFile);

            log.info("Successfully analyzed metadata for: {} - Video: {}, Audio: {}, Format: {}",
                mediaFile.getPath(),
                metadata.getVideoCodec(),
                metadata.getAudioCodec(),
                metadata.getContainerFormat());
        } catch (IOException e) {
            log.error("Failed to analyze video metadata for: {}", mediaFile.getPath(), e);
            // Don't throw exception - allow the file to be served even without metadata
            // Set metadataAnalyzed to true to prevent repeated failed attempts
            mediaFile.setMetadataAnalyzed(true);
            persistenceService.saveMediaFile(mediaFile);
        }
    }
}

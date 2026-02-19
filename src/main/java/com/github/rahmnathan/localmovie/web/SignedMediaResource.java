package com.github.rahmnathan.localmovie.web;

import com.github.rahmnathan.localmovie.persistence.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.MediaSubtitleRepository;
import com.google.api.client.http.HttpStatusCodes;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@AllArgsConstructor
@CrossOrigin(
    origins = "*",
    allowedHeaders = "*",
    exposedHeaders = {"Content-Range", "Accept-Ranges", "Content-Length", "Content-Type"},
    methods = {RequestMethod.GET, RequestMethod.PATCH, RequestMethod.OPTIONS, RequestMethod.HEAD}
)
@RestController
@RequestMapping(value = "/localmovie/v1/signed/media")
public class SignedMediaResource {
    private final MediaPersistenceService persistenceService;
    private final MediaStreamingService mediaStreamingService;
    private final SecurityService securityService;
    private final MediaSubtitleRepository subtitleRepository;

    @GetMapping(value = {"/{mediaFileId}/stream", "/{mediaFileId}/stream.mp4"})
    public ResponseEntity<ResourceRegion> streamSecureVideo(@PathVariable String mediaFileId,
                                                            @RequestParam(value = "expires", defaultValue = "0") long expires,
                                                            @RequestParam(value = "sig") String signature,
                                                            @RequestHeader HttpHeaders headers) {
        log.info("Received streaming request - {}", mediaFileId);

        if (!securityService.authorizedRequest(mediaFileId, expires, signature)) {
            log.warn("Unauthorized stream request for id.");
            return ResponseEntity.status(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED).build();
        }

        Optional<MediaFile> mediaFilePath = persistenceService.findByMediaFileId(mediaFileId);
        if(mediaFilePath.isEmpty()){
            log.warn("Media file not found for id.");
            return ResponseEntity.notFound().build();
        }

        return mediaStreamingService.streamMediaFile(mediaFilePath.get(), headers);
    }

    @GetMapping(path = "/{mediaFileId}/poster")
    public ResponseEntity<byte[]> getPoster(@PathVariable String mediaFileId) {
        log.info("Streaming poster - {}", mediaFileId);
        byte[] image = persistenceService.getMediaImageById(mediaFileId);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=86400") // Cache for 24 hours
                .header("Content-Type", "image/jpeg")
                .body(image);
    }

    @PatchMapping(path = "/{mediaFileId}/position/{position}")
    public ResponseEntity<Void> updatePosition(@PathVariable String mediaFileId,
                                               @PathVariable Double position,
                                               @RequestParam(value = "expires", defaultValue = "0") long expires,
                                               @RequestParam(value = "user", required = false) String userId,
                                               @RequestParam(value = "sig") String signature,
                                               @RequestParam(value = "duration", required = false) Double duration) {
        log.info("Updating position for MediaFile id: {} user: {} position: {} duration: {}", mediaFileId, userId, position, duration);

        if (!securityService.authorizedRequest(mediaFileId, userId, expires, signature)) {
            log.warn("Unauthorized position update request.");
            return ResponseEntity.status(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED).build();
        }

        persistenceService.addView(mediaFileId, userId, position, duration);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/{mediaFileId}/subtitle.vtt", produces = "text/vtt")
    public ResponseEntity<String> getSubtitle(@PathVariable String mediaFileId,
                                              @RequestParam(value = "expires", defaultValue = "0") long expires,
                                              @RequestParam(value = "sig") String signature) {
        log.info("Received subtitle request - {}", mediaFileId);

        if (!securityService.authorizedRequest(mediaFileId, expires, signature)) {
            log.warn("Unauthorized subtitle request for id.");
            return ResponseEntity.status(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED).build();
        }

        Optional<String> subtitleContent = subtitleRepository
                .findSubtitleContentByMediaFileIdAndLanguage(mediaFileId, "en");

        if (subtitleContent.isEmpty()) {
            log.info("No subtitle found for MediaFile: {}", mediaFileId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header("Content-Type", "text/vtt; charset=utf-8")
                .body(subtitleContent.get());
    }
}

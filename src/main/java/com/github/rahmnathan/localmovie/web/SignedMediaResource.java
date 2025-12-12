package com.github.rahmnathan.localmovie.web;

import com.github.rahmnathan.localmovie.persistence.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
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
@CrossOrigin
@RestController
@RequestMapping(value = "/localmovie/v1/signed/media")
public class SignedMediaResource {
    private final MediaPersistenceService persistenceService;
    private final MediaStreamingService mediaStreamingService;
    private final SecurityService securityService;

    @GetMapping(value = "/{mediaFileId}/stream.mp4", produces = "video/mp4")
    public ResponseEntity<ResourceRegion> streamSecureVideo(@PathVariable("mediaFileId") String mediaFileId,
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
    public ResponseEntity<byte[]> getPoster(@PathVariable("mediaFileId") String mediaFileId,
                            @RequestParam(value = "expires", defaultValue = "0") long expires,
                            @RequestParam(value = "sig") String signature) {
        log.info("Streaming poster - {}", mediaFileId);

        if (!securityService.authorizedRequest(mediaFileId, expires, signature)) {
            log.warn("Unauthorized poster request for id.");
            return ResponseEntity.status(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(persistenceService.getMediaImageById(mediaFileId));
    }

    @PatchMapping(path = "/{mediaFileId}/position/{position}")
    public ResponseEntity<Void> updatePosition(@PathVariable("mediaFileId") String mediaFileId,
                                               @PathVariable("position") Double position,
                                               @RequestParam(value = "expires", defaultValue = "0") long expires,
                                               @RequestParam(value = "sig") String signature) {
        log.info("Updating position for MediaFile id: {} position: {}", mediaFileId, position);

        if (!securityService.authorizedRequest(mediaFileId, expires, signature)) {
            log.warn("Unauthorized poster request for id.");
            return ResponseEntity.status(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED).build();
        }

        persistenceService.addView(mediaFileId, position);
        return ResponseEntity.ok().build();
    }
}

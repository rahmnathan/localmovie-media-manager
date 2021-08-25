package com.github.rahmnathan.localmovie.web;

import com.github.rahmnathan.localmovie.persistence.control.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(value = "/localmovie/v3/media")
public class MediaResourceV3 {
    private final MediaPersistenceService persistenceService;
    private final FileSenderService fileSenderService;

    @GetMapping(value = "/{mediaFileId}", produces= MediaType.APPLICATION_JSON_VALUE)
    public Optional<MediaFile> getMedia(@PathVariable("mediaFileId") String mediaFileId) {
        log.info("Received media request for id - {}", mediaFileId);
        return persistenceService.getMediaFileByIdWithViews(mediaFileId);
    }

    @GetMapping(value = "/{mediaFileId}/stream.mp4", produces = "video/mp4")
    public void streamVideo(@PathVariable("mediaFileId") String mediaFileId, HttpServletResponse response, HttpServletRequest request) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader(HttpHeaders.CONTENT_TYPE, "video/mp4");
        log.info("Received streaming request - {}", mediaFileId);

        Optional<MediaFile> mediaFileOptional = persistenceService.getMediaFileById(mediaFileId);
        if(mediaFileOptional.isEmpty()){
            log.warn("Media file not found for id.");
            return;
        }

        fileSenderService.streamMediaFile(mediaFileOptional.get(), request, response);
    }

    @GetMapping(path = "/{mediaFileId}/poster")
    public byte[] getPoster(@PathVariable("mediaFileId") String id) {
        log.info("Streaming poster - {}", id);

        return persistenceService.getMediaImageById(id);
    }

    @PatchMapping(path = "/{mediaFileId}/position/{position}")
    public void updatePosition(@PathVariable("mediaFileId") String id, @PathVariable("position") Double position) {
        log.info("Updating position for MediaFile id: {} position: {}", id, position);

        persistenceService.addView(id, position);
    }
}
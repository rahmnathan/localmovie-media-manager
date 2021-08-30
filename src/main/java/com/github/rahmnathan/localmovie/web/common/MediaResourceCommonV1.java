package com.github.rahmnathan.localmovie.web.common;

import com.github.rahmnathan.localmovie.persistence.control.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(value = "/localmovie/v1/media")
public class MediaResourceCommonV1 {
    private static final String RESPONSE_HEADER_COUNT = "Count";
    private final MediaPersistenceService persistenceService;
    private final MediaStreamingService mediaStreamingService;

    @GetMapping(value = "/{mediaFileId}", produces= MediaType.APPLICATION_JSON_VALUE)
    public Optional<MediaFile> getMedia(@PathVariable("mediaFileId") String mediaFileId) {
        log.info("Received media request for id - {}", mediaFileId);
        return persistenceService.getMediaFileByIdWithViews(mediaFileId);
    }

    @GetMapping(value = "/count")
    public void getMediaCount(@RequestParam(value = "path") String path, HttpServletResponse response){
        log.info("Received count request for path - {}", path);

        long count = persistenceService.countMediaFiles(path);

        log.info("Returning count of - {}", count);
        response.setHeader(RESPONSE_HEADER_COUNT, String.valueOf(count));
    }

    @GetMapping(value = "/{mediaFileId}/stream.mp4", produces = "video/mp4")
    public void streamVideo(@PathVariable("mediaFileId") String mediaFileId, HttpServletResponse response, HttpServletRequest request) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader(HttpHeaders.CONTENT_TYPE, "video/mp4");
        log.info("Received streaming request - {}", mediaFileId);

        Optional<MediaFile> mediaFileOptional = persistenceService.getMediaFileById(mediaFileId);
        if(mediaFileOptional.isEmpty()){
            log.warn("Media file not found for id.");
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        mediaStreamingService.streamMediaFile(mediaFileOptional.get(), request, response);
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

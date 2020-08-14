package com.github.rahmnathan.localmovie.web;

import com.github.rahmnathan.localmovie.persistence.control.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.Paths;
import java.util.Optional;

@RestController
@RequestMapping(value = "/localmovie/v3/media")
public class MediaResourceV3 {
    private final Logger logger = LoggerFactory.getLogger(MediaResourceV3.class.getName());
    private final MediaPersistenceService persistenceService;
    private final FileSenderService fileSenderService;

    public MediaResourceV3(MediaPersistenceService persistenceService, FileSenderService fileSenderService){
        this.persistenceService = persistenceService;
        this.fileSenderService = fileSenderService;
    }

    @GetMapping(value = "/{mediaFileId}/stream.mp4", produces = "video/mp4")
    public void streamVideo(@PathVariable("mediaFileId") String mediaFileId, HttpServletResponse response, HttpServletRequest request) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader(HttpHeaders.CONTENT_TYPE, "video/mp4");
        logger.info("Received streaming request - {}", mediaFileId);

        Optional<MediaFile> mediaFileOptional = persistenceService.getMediaFileById(mediaFileId);
        if(mediaFileOptional.isEmpty()){
            logger.warn("Media file not found for id.");
            return;
        }

        fileSenderService.serveResource(mediaFileOptional.get(), request, response);
    }

    @GetMapping(path = "/{mediaFileId}/poster")
    public byte[] getPoster(@PathVariable("mediaFileId") String id) {
        logger.info("Streaming poster - {}", id);

        return persistenceService.getMediaImageById(id);
    }
}
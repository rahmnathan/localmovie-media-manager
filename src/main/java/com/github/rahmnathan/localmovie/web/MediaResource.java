package com.github.rahmnathan.localmovie.web;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.data.MediaClient;
import com.github.rahmnathan.localmovie.persistence.control.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
import com.github.rahmnathan.localmovie.persistence.entity.RedactedMediaFile;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import io.micrometer.core.instrument.Timer;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(value = "/localmovie/v2/media")
public class MediaResource {
    private final Logger logger = LoggerFactory.getLogger(MediaResource.class.getName());
    private final MediaPersistenceService persistenceService;
    private final FileSenderService fileSenderService;
    private final Set<String> mediaPaths;

    public MediaResource(MediaPersistenceService persistenceService, ServiceConfig serviceConfig, FileSenderService fileSenderService){
        this.mediaPaths = serviceConfig.getMediaPaths();
        this.persistenceService = persistenceService;
        this.fileSenderService = fileSenderService;
    }

    @PostMapping(produces=MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List> getMedia(@RequestBody MediaRequest mediaRequest, HttpServletResponse response) {
        logger.info("Received request: {}", mediaRequest.toString());

        if(mediaRequest.getPage() == 0)
            getMediaCount(mediaRequest.getPath(), response);

        if(mediaRequest.getClient() == MediaClient.WEBAPP){
            logger.info("Loading redacted media files for webapp.");
            List<RedactedMediaFile> redactedMediaFiles = persistenceService.getMediaFilesByParentPathNoPoster(mediaRequest);
            logger.info("Returning media list. Size: {}", redactedMediaFiles.size());
            return ResponseEntity.ok(redactedMediaFiles);
        } else {
            logger.info("Loading full media files for Android.");
            List<MediaFile> mediaFiles = persistenceService.getMediaFilesByParentPath(mediaRequest);
            logger.info("Returning media list. Size: {}", mediaFiles.size());
            return ResponseEntity.ok(mediaFiles);
        }
    }

    @GetMapping(value = "/count")
    public void getMediaCount(@RequestParam(value = "path") String path, HttpServletResponse response){
        logger.info("Received count request for path - {}", path);

        long count = persistenceService.countMediaFiles(path);

        logger.info("Returning count of - {}", count);
        response.setHeader("Count", String.valueOf(count));
    }

    /**
     * @param path - Path to video file to stream
     */
    @GetMapping(value = "/stream.mp4", produces = "video/mp4")
    public void streamVideo(@RequestParam("path") String path, HttpServletResponse response, HttpServletRequest request) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader(HttpHeaders.CONTENT_TYPE, "video/mp4");
        logger.info("Received streaming request - {}", path);

        if(!persistenceService.existsByPath(path)) return;

        boolean found = false;
        for (String mediaPath : mediaPaths) {
            logger.info("Checking if mediaPath {} contains requested path {}", mediaPath, path);
            if (new File(mediaPath + path).exists()) {
                logger.info("Streaming - {}{}", mediaPath, path);
                found = true;
                fileSenderService.serveResource(Paths.get(mediaPath + path), request, response);
                break;
            }
        }
        if (!found) {
            logger.warn("Path not found in mediaPaths: {}", path);
        }
    }

    /**
     * @param path - Path to video file
     * @return - Poster image for specified video file
     */
    @GetMapping(path = "/poster")
    public byte[] getPoster(@RequestParam("path") String path) {
        logger.info("Streaming poster - {}", path);

        return persistenceService.getMediaImage(path);
    }

    /**
     * @param epoch - Timestamp to collect events since
     * @return - List of MediaFileEvents
     */
    @GetMapping(path = "/events")
    public List<MediaFileEvent> getEvents(@RequestParam("timestamp") Long epoch) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault());
        logger.info("Request for events since: {}", localDateTime);

        List<MediaFileEvent> events = persistenceService.getMediaFileEvents(localDateTime);

        logger.info("Events response. Time: {} EventList: {}", localDateTime, events);
        return events;
    }
}
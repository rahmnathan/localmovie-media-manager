package com.github.rahmnathan.localmovie.web;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.data.MediaClient;
import com.github.rahmnathan.localmovie.persistence.control.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
import com.github.rahmnathan.localmovie.persistence.entity.RedactedMediaFile;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping(value = "/localmovie/v2/media")
public class MediaResource {
    private static final String RESPONSE_HEADER_COUNT = "Count";
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
        log.info("Received request: {}", mediaRequest.toString());

        if(mediaRequest.getPage() == 0)
            getMediaCount(mediaRequest.getPath(), response);

        if(mediaRequest.getClient() == MediaClient.WEBAPP){
            log.info("Loading redacted media files for webapp.");
            List<RedactedMediaFile> redactedMediaFiles = persistenceService.getMediaFilesByParentPathNoPoster(mediaRequest);
            log.info("Returning media list. Size: {}", redactedMediaFiles.size());
            return ResponseEntity.ok(redactedMediaFiles);
        } else {
            log.info("Loading full media files for Android.");
            List<MediaFile> mediaFiles = persistenceService.getMediaFilesByParentPath(mediaRequest);
            log.info("Returning media list. Size: {}", mediaFiles.size());
            return ResponseEntity.ok(mediaFiles);
        }
    }

    @GetMapping(value = "/count")
    public void getMediaCount(@RequestParam(value = "path") String path, HttpServletResponse response){
        log.info("Received count request for path - {}", path);

        long count = persistenceService.countMediaFiles(path);

        log.info("Returning count of - {}", count);
        response.setHeader(RESPONSE_HEADER_COUNT, String.valueOf(count));
    }

    /**
     * @param path - Path to video file to stream
     */
    @GetMapping(value = "/stream.mp4", produces = "video/mp4")
    public void streamVideo(@RequestParam("path") String path, HttpServletResponse response, HttpServletRequest request) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader(HttpHeaders.CONTENT_TYPE, "video/mp4");
        log.info("Received streaming request - {}", path);

        if(!persistenceService.existsByPath(path)) return;

        boolean found = false;
        for (String mediaPath : mediaPaths) {
            log.info("Checking if mediaPath {} contains requested path {}", mediaPath, path);
            if (new File(mediaPath + path).exists()) {
                log.info("Streaming - {}{}", mediaPath, path);
                found = true;
                fileSenderService.serveResource(MediaFile.Builder.forPath(mediaPath + path).build(), request, response);
                break;
            }
        }
        if (!found) {
            log.warn("Path not found in mediaPaths: {}", path);
        }
    }

    /**
     * @param path - Path to video file
     * @return - Poster image for specified video file
     */
    @GetMapping(path = "/poster")
    public byte[] getPoster(@RequestParam("path") String path) {
        log.info("Streaming poster - {}", path);

        return persistenceService.getMediaImage(path);
    }

    /**
     * @param epoch - Timestamp to collect events since
     * @return - List of MediaFileEvents
     */
    @GetMapping(path = "/events")
    public List<MediaFileEvent> getEvents(@RequestParam("timestamp") Long epoch, Pageable pageable, HttpServletResponse response) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault());
        log.info("Request for events since: {}", localDateTime);

        if(pageable.isPaged() && pageable.getPageNumber() == 0)
            countEvents(epoch, response);

        List<MediaFileEvent> events = persistenceService.getMediaFileEvents(localDateTime, pageable);

        log.info("Events response. Time: {} EventList: {}", localDateTime, events);
        return events;
    }

    /**
     * @param epoch - Timestamp to count events since
     */
    @GetMapping(path = "/events/count")
    public void countEvents(@RequestParam("timestamp") Long epoch, HttpServletResponse response) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault());
        log.info("Request for event count since: {}", localDateTime);

        long eventCount = persistenceService.getMediaFileEventCount(localDateTime);

        log.info("Event count response. Time: {} Event Count: {}", localDateTime, eventCount);
        response.setHeader(RESPONSE_HEADER_COUNT, String.valueOf(eventCount));
    }
}
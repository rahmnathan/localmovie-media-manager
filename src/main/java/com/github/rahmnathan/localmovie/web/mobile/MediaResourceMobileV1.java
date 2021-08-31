package com.github.rahmnathan.localmovie.web.mobile;

import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.persistence.control.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "/localmovie/mobile/v1/media")
public class MediaResourceMobileV1 {
    static final String RESPONSE_HEADER_COUNT = "Count";
    private final MediaPersistenceService persistenceService;

    public MediaResourceMobileV1(MediaPersistenceService persistenceService){
        this.persistenceService = persistenceService;
    }

    @PostMapping(produces=MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<MediaFile> getMedia(@RequestBody MediaRequest mediaRequest, HttpServletResponse response) {
        log.info("Received request: {}", mediaRequest.toString());

        if(mediaRequest.getPage() == 0)
            getMediaCount(mediaRequest.getPath(), response);

        log.info("Loading media files for mobile.");
        List<MediaFile> mediaFiles = persistenceService.getMediaFilesByParentPath(mediaRequest);
        log.info("Returning media list. Size: {}", mediaFiles.size());
        return mediaFiles;
    }

    @GetMapping(value = "/count")
    public void getMediaCount(@RequestParam(value = "path") String path, HttpServletResponse response){
        log.info("Received count request for path - {}", path);

        long count = persistenceService.countMediaFiles(path);

        log.info("Returning count of - {}", count);
        response.setHeader(RESPONSE_HEADER_COUNT, String.valueOf(count));
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
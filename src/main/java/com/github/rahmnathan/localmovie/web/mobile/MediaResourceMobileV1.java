package com.github.rahmnathan.localmovie.web.mobile;

import com.github.rahmnathan.localmovie.data.MediaEventDto;
import com.github.rahmnathan.localmovie.data.MediaFileDto;
import com.github.rahmnathan.localmovie.data.transformer.MediaFileTransformer;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.persistence.MediaPersistenceService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @PostMapping(value = "/history", produces=MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<MediaFileDto> getMediaHistory(@RequestBody MediaRequest mediaRequest, HttpServletResponse response) {
        log.info("Received history request: {}", mediaRequest.toString());
        handleDemoUser(mediaRequest);

        if(mediaRequest.getPage() == 0)
            getMediaHistoryCount(mediaRequest, response);

        log.info("Loading media history for mobile.");
        List<MediaFileDto> mediaFiles = persistenceService.getMediaHistory(mediaRequest, true);
        log.info("Returning media history list. Size: {}", mediaFiles.size());
        return mediaFiles;
    }

    @PostMapping(value = "/history/count")
    public void getMediaHistoryCount(@RequestBody MediaRequest mediaRequest, HttpServletResponse response){
        log.info("Received history count request: {}", mediaRequest);

        long count = persistenceService.countHistory();

        log.info("Returning history count of - {}", count);
        response.setHeader(RESPONSE_HEADER_COUNT, String.valueOf(count));
    }

    @PostMapping(produces=MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<MediaFileDto> getMedia(@RequestBody MediaRequest mediaRequest, HttpServletResponse response) {
        log.info("Received request: {}", mediaRequest.toString());
        handleDemoUser(mediaRequest);

        if(mediaRequest.getPage() == 0)
            getMediaCount(mediaRequest, response);

        log.info("Loading media files for mobile.");
        List<MediaFileDto> mediaFiles = persistenceService.getMediaFiles(mediaRequest, true);
        log.info("Returning media list. Size: {}", mediaFiles.size());
        return mediaFiles;
    }

    @PostMapping(value = "/count")
    public void getMediaCount(@RequestBody MediaRequest mediaRequest, HttpServletResponse response){
        log.info("Received count request: {}", mediaRequest);

        long count = persistenceService.countMediaFiles(mediaRequest);

        log.info("Returning count of - {}", count);
        response.setHeader(RESPONSE_HEADER_COUNT, String.valueOf(count));
    }

    /**
     * @param epoch - Timestamp to collect events since
     * @return - List of MediaFileEvents
     */
    @GetMapping(path = "/events")
    public List<MediaEventDto> getEvents(@RequestParam("timestamp") Long epoch, Pageable pageable, HttpServletResponse response) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault());
        log.info("Request for events since: {}", localDateTime);

        if(pageable.isPaged() && pageable.getPageNumber() == 0)
            countEvents(epoch, response);

        if(isDemoUser()) {
            return List.of();
        }

        List<MediaEventDto> events = persistenceService.getMediaFileEvents(localDateTime, pageable).stream()
                .map(MediaFileTransformer::toMediaEventDto)
                .toList();

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

        long eventCount = 0;
        if(!isDemoUser()) {
            eventCount = persistenceService.getMediaFileEventCount(localDateTime);
        }

        log.info("Event count response. Time: {} Event Count: {}", localDateTime, eventCount);
        response.setHeader(RESPONSE_HEADER_COUNT, String.valueOf(eventCount));
    }

    private void handleDemoUser(MediaRequest mediaRequest) {
        if(isDemoUser()) {
            mediaRequest.setQ("Big Buck Bunny");
        }
    }

    private boolean isDemoUser() {
        return "demouser".equalsIgnoreCase(getUsername()) || "def8e370-c7e6-4fc8-9301-7a40765927db".equalsIgnoreCase(getUsername());
    }

    private String getUsername(){
        String username = "movieuser";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null){
            username = authentication.getName();
        }

        log.info("Username: {}", username);

        return username;
    }
}
package com.github.rahmnathan.localmovie.web.webapp;

import com.github.rahmnathan.localmovie.data.MediaFileDto;
import com.github.rahmnathan.localmovie.data.MediaFileTransformer;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.persistence.control.MediaPersistenceService;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

// Test push

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(value = "/localmovie/v1/media")
public class MediaResource {
    private static final String RESPONSE_HEADER_COUNT = "Count";
    private final MediaPersistenceService persistenceService;
    private final MediaStreamingService mediaStreamingService;

    @PostMapping(produces= MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<MediaFileDto> getMedia(@RequestBody MediaRequest mediaRequest, HttpServletResponse response) {
        log.info("Received request: {}", mediaRequest.toString());
        handleDemoUser(mediaRequest);

        if(mediaRequest.getPage() == 0)
            getMediaCount(mediaRequest, response);

        log.info("Loading media files for webapp.");
        List<MediaFileDto> mediaFiles = persistenceService.getMediaFiles(mediaRequest, false);
        log.info("Returning media list. Size: {}", mediaFiles.size());
        return mediaFiles;
    }

    @PostMapping(value = "/history", produces= MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<MediaFileDto> getMediaHistory(@RequestBody MediaRequest mediaRequest, HttpServletResponse response) {
        log.info("Received history request: {}", mediaRequest.toString());
        handleDemoUser(mediaRequest);

        if(mediaRequest.getPage() == 0)
            getMediaHistoryCount(response);

        log.info("Loading history media files for webapp.");
        List<MediaFileDto> mediaFiles = persistenceService.getMediaHistory(mediaRequest, false);
        log.info("Returning history media list. Size: {}", mediaFiles.size());
        return mediaFiles;
    }

    @PostMapping(value = "/history/count")
    public void getMediaHistoryCount(HttpServletResponse response){

        long count = persistenceService.countHistory();

        log.info("Returning history count of - {}", count);
        response.setHeader(RESPONSE_HEADER_COUNT, String.valueOf(count));
    }

    @GetMapping(value = "/{mediaFileId}", produces= MediaType.APPLICATION_JSON_VALUE)
    public Optional<MediaFileDto> getMedia(@PathVariable("mediaFileId") String mediaFileId) {
        log.info("Received media request for id - {}", mediaFileId);
        return persistenceService.getMediaFileByIdWithViews(mediaFileId)
                .map(mediaFile -> MediaFileTransformer.toMediaFileDto(mediaFile, false));
    }

    @PostMapping(value = "/count")
    public void getMediaCount(@RequestBody MediaRequest mediaRequest, HttpServletResponse response){
        log.info("Received count request: {}", mediaRequest);

        long count = persistenceService.count(mediaRequest);

        log.info("Returning count of - {}", count);
        response.setHeader(RESPONSE_HEADER_COUNT, String.valueOf(count));
    }

    @GetMapping(value = "/{mediaFileId}/stream.mp4", produces = "video/mp4")
    public void streamVideo(@PathVariable("mediaFileId") String mediaFileId, HttpServletResponse response, HttpServletRequest request) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader(HttpHeaders.CONTENT_TYPE, "video/mp4");
        log.info("Received streaming request - {}", mediaFileId);

        Optional<String> mediaFilePath = persistenceService.getMediaFilePathById(mediaFileId);
        if(mediaFilePath.isEmpty()){
            log.warn("Media file not found for id.");
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        mediaStreamingService.streamMediaFile(mediaFilePath.get(), request, response);
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

    private void handleDemoUser(MediaRequest mediaRequest) {
        if("demouser".equalsIgnoreCase(getUsername()) || "67b9e1d6-e817-45b6-b532-bc626e642faa".equalsIgnoreCase(getUsername())) {
            mediaRequest.setQ("Big Buck Bunny");
        }
    }

    private String getUsername(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null){
            return authentication.getName();
        }

        return "movieuser";
    }
}

package com.github.rahmnathan.localmovie.web.webapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.rahmnathan.localmovie.data.MediaFileDto;
import com.github.rahmnathan.localmovie.data.SignedUrls;
import com.github.rahmnathan.localmovie.data.transformer.MediaFileTransformer;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.persistence.MediaPersistenceService;

import java.util.List;
import java.util.Optional;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(value = "/localmovie/v1/media")
public class MediaResource {
    private static final String RESPONSE_HEADER_COUNT = "Count";
    private final MediaPersistenceService persistenceService;
    private final SecurityService securityService;

    @PostMapping(produces= MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<MediaFileDto> getMedia(@RequestBody @Valid MediaRequest mediaRequest, HttpServletResponse response) throws JsonProcessingException {
        log.info("Received request: {}", mediaRequest.toString());
        handleDemoUser(mediaRequest);

        if(mediaRequest.getPage() == 0)
            getMediaCount(mediaRequest, response);

        log.info("Loading media files for webapp.");
        List<MediaFileDto> mediaFiles = persistenceService.getMediaFiles(mediaRequest, false);
        mediaFiles.forEach(mediaFileDto -> mediaFileDto.setSignedUrls(securityService.generateSignedPosterUrl(mediaFileDto.getMediaFileId())));
        log.info("Returning media list. Size: {}", mediaFiles.size());
        return mediaFiles;
    }

    @GetMapping(value = "/{mediaFileId}", produces= MediaType.APPLICATION_JSON_VALUE)
    public Optional<MediaFileDto> getMedia(@PathVariable("mediaFileId") String mediaFileId) throws JsonProcessingException {
        log.info("Received media request for id - {}", mediaFileId);
        return persistenceService.getMediaFileByIdWithViews(mediaFileId)
                .map(mediaFile -> MediaFileTransformer.toMediaFileDto(mediaFile, false))
                .map(mediaFileDto -> {
                    mediaFileDto.setSignedUrls(securityService.generateSignedPosterUrl(mediaFileDto.getMediaFileId()));
                    return mediaFileDto;
                });
    }

    @PostMapping(value = "/count")
    public void getMediaCount(@RequestBody MediaRequest mediaRequest, HttpServletResponse response){
        log.info("Received count request: {}", mediaRequest);

        long count = persistenceService.countMediaFiles(mediaRequest);

        log.info("Returning count of - {}", count);
        response.setHeader(RESPONSE_HEADER_COUNT, String.valueOf(count));
    }

    @GetMapping(value = "/{mediaFileId}/url/signed")
    public ResponseEntity<SignedUrls> getSignedUrls(@PathVariable("mediaFileId") String mediaFileId) {
        log.info("Received request to generate stream url - {}", mediaFileId);

        Optional<MediaFile> mediaFilePath = persistenceService.findByMediaFileId(mediaFileId);
        if(mediaFilePath.isEmpty()){
            log.warn("Media file not found for id.");
            return ResponseEntity.notFound().build();
        }

        try {
            return ResponseEntity.ok(securityService.generateSignedUrls(mediaFileId));
        } catch (JsonProcessingException e) {
            log.error("Failed to generate signed URLs.", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(path = "/{mediaFileId}/poster")
    public byte[] getPoster(@PathVariable("mediaFileId") String id) {
        log.info("Streaming poster - {}", id);

        return persistenceService.getMediaImageById(id);
    }

    private void handleDemoUser(MediaRequest mediaRequest) {
        if("demouser".equalsIgnoreCase(getUsername()) || "def8e370-c7e6-4fc8-9301-7a40765927db".equalsIgnoreCase(getUsername())) {
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

package com.github.rahmnathan.localmovie.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.rahmnathan.localmovie.data.MediaFileDto;
import com.github.rahmnathan.localmovie.data.SignedUrls;
import com.github.rahmnathan.localmovie.data.transformer.MediaFileTransformer;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.persistence.MediaPersistenceService;

import java.util.List;
import java.util.Optional;

import com.github.rahmnathan.localmovie.persistence.MediaViewService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.persistence.repository.MediaSubtitleRepository;
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
    private final MediaSubtitleRepository subtitleRepository;
    private final MediaViewService mediaViewService;

    @PostMapping(produces= MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<MediaFileDto> getMedia(@RequestBody @Valid MediaRequest mediaRequest, HttpServletResponse response) {
        log.info("Received request: {}", mediaRequest.toString());
        handleDemoUser(mediaRequest);

        if(mediaRequest.getPage() == 0)
            getMediaCount(mediaRequest, response);

        log.info("Loading media files for webapp.");
        List<MediaFileDto> mediaFiles = persistenceService.getMediaFileDtos(mediaRequest);
        log.info("Returning media list. Size: {}", mediaFiles.size());
        return mediaFiles;
    }

    @GetMapping(value = "/{mediaFileId}", produces= MediaType.APPLICATION_JSON_VALUE)
    public Optional<MediaFileDto> getMedia(@PathVariable("mediaFileId") String mediaFileId) {
        log.info("Received media request for id - {}", mediaFileId);
        return persistenceService.getMediaFileByIdWithViews(mediaFileId)
                .map(MediaFileTransformer::toMediaFileDto);
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

        boolean hasSubtitle = subtitleRepository.existsByMediaFileUuid(mediaFileId);

        try {
            return ResponseEntity.ok(securityService.generateSignedUrls(mediaFileId, hasSubtitle));
        } catch (JsonProcessingException e) {
            log.error("Failed to generate signed URLs.", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(path = "/{mediaFileId}/poster")
    public ResponseEntity<byte[]> getPoster(@PathVariable("mediaFileId") String id) {
        log.info("Streaming poster - {}", id);
        byte[] image = persistenceService.getMediaImageById(id);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=86400") // Cache for 24 hours
                .header("Content-Type", "image/jpeg")
                .body(image);
    }

    @PostMapping(path = "/{mediaFileId}/favorite")
    public ResponseEntity<Void> addFavorite(@PathVariable("mediaFileId") String mediaFileId) {
        log.info("Adding favorite - {}", mediaFileId);
        persistenceService.addFavorite(mediaFileId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(path = "/{mediaFileId}/favorite")
    public ResponseEntity<Void> removeFavorite(@PathVariable("mediaFileId") String mediaFileId) {
        log.info("Removing favorite - {}", mediaFileId);
        persistenceService.removeFavorite(mediaFileId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(path = "/history")
    public ResponseEntity<Void> clearHistory() {
        log.info("Clearing history");
        mediaViewService.clearHistory();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(path = "/{mediaFileId}/history")
    public ResponseEntity<Void> removeFromHistory(@PathVariable("mediaFileId") String mediaFileId) {
        log.info("Removing from history - {}", mediaFileId);
        mediaViewService.removeFromHistory(mediaFileId);
        return ResponseEntity.ok().build();
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

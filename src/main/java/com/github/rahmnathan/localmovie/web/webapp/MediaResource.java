package com.github.rahmnathan.localmovie.web.webapp;

import com.github.rahmnathan.localmovie.data.MediaFileDto;
import com.github.rahmnathan.localmovie.data.transformer.MediaFileTransformer;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.persistence.MediaPersistenceService;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import com.google.api.client.http.HttpStatusCodes;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.bind.annotation.*;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(value = "/localmovie/v1/media")
public class MediaResource {
    private static final String RESPONSE_HEADER_COUNT = "Count";
    private final MediaPersistenceService persistenceService;
    private final MediaStreamingService mediaStreamingService;
    private final SecurityService securityService;

    @PostMapping(produces= MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<MediaFileDto> getMedia(@RequestBody @Valid MediaRequest mediaRequest, HttpServletResponse response) {
        log.info("Received request: {}", mediaRequest.toString());
        handleDemoUser(mediaRequest);

        if(mediaRequest.getPage() == 0)
            getMediaCount(mediaRequest, response);

        log.info("Loading media files for webapp.");
        List<MediaFileDto> mediaFiles = persistenceService.getMediaFiles(mediaRequest, false);
        log.info("Returning media list. Size: {}", mediaFiles.size());
        return mediaFiles;
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

        long count = persistenceService.countMediaFiles(mediaRequest);

        log.info("Returning count of - {}", count);
        response.setHeader(RESPONSE_HEADER_COUNT, String.valueOf(count));
    }

    @GetMapping(value = "/{mediaFileId}/stream.mp4", produces = "video/mp4")
    public ResponseEntity<ResourceRegion> streamVideo(@PathVariable("mediaFileId") String mediaFileId,
                                                      @RequestHeader HttpHeaders headers) {
        log.info("Received streaming request - {}", mediaFileId);

        Optional<MediaFile> mediaFilePath = persistenceService.findByMediaFileId(mediaFileId);
        if(mediaFilePath.isEmpty()){
            log.warn("Media file not found for id.");
            return ResponseEntity.notFound().build();
        }

        return mediaStreamingService.streamMediaFile(mediaFilePath.get(), headers);
    }

    @GetMapping(value = "/{mediaFileId}/signed/stream.mp4", produces = "video/mp4")
    public ResponseEntity<ResourceRegion> streamSecureVideo(@PathVariable("mediaFileId") String mediaFileId,
                                                      @RequestParam(value = "expires", defaultValue = "0") long expires,
                                                      @RequestParam(value = "sig") String signature,
                                                      @RequestHeader HttpHeaders headers) {
        log.info("Received streaming request - {}", mediaFileId);

        if (!securityService.authorizedRequest(mediaFileId, expires, signature)) {
            log.warn("Unauthorized stream request for id.");
            return ResponseEntity.status(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED).build();
        }

        Optional<MediaFile> mediaFilePath = persistenceService.findByMediaFileId(mediaFileId);
        if(mediaFilePath.isEmpty()){
            log.warn("Media file not found for id.");
            return ResponseEntity.notFound().build();
        }

        return mediaStreamingService.streamMediaFile(mediaFilePath.get(), headers);
    }

    @GetMapping(value = "/{mediaFileId}/url/signed")
    public ResponseEntity<String> streamSecureVideo(@PathVariable("mediaFileId") String mediaFileId) {
        log.info("Received request to generate stream url - {}", mediaFileId);

        Optional<MediaFile> mediaFilePath = persistenceService.findByMediaFileId(mediaFileId);
        if(mediaFilePath.isEmpty()){
            log.warn("Media file not found for id.");
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.created(URI.create(securityService.generateSignedUrl(mediaFileId))).build();
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

    @GetMapping("/token")
    public String getToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof DefaultOidcUser user) {
            if (user.getIdToken() != null) {
                return user.getIdToken().getTokenValue();
            }
        }

        return "";
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

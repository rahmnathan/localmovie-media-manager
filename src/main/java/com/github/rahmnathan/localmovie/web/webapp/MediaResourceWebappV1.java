package com.github.rahmnathan.localmovie.web.webapp;

import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.persistence.control.MediaPersistenceService;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFileNoPoster;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(value = "/localmovie/webapp/v1/media")
public class MediaResourceWebappV1 {
    private static final String RESPONSE_HEADER_COUNT = "Count";
    private final MediaPersistenceService persistenceService;

    @PostMapping(produces= MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<MediaFileNoPoster> getMedia(@RequestBody MediaRequest mediaRequest, HttpServletResponse response) {
        log.info("Received request: {}", mediaRequest.toString());

        if(mediaRequest.getPage() == 0)
            getMediaCount(mediaRequest.getPath(), response);

        log.info("Loading media files for webapp.");
        List<MediaFileNoPoster> mediaFiles = persistenceService.getMediaFilesByParentPathNoPoster(mediaRequest);
        log.info("Returning media list. Size: {}", mediaFiles.size());
        return mediaFiles;
    }

    private void getMediaCount(String path, HttpServletResponse response){
        log.info("Received count request for path - {}", path);

        long count = persistenceService.countMediaFiles(path);

        log.info("Returning count of - {}", count);
        response.setHeader(RESPONSE_HEADER_COUNT, String.valueOf(count));
    }
}

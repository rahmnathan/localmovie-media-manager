package com.github.rahmnathan.localmovie.web.admin;

import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.media.MediaUpdateService;
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
@RequestMapping(value = "/admin/v1")
public class MediaAdminResource {
    private final MediaUpdateService updateService;

    @PostMapping(path = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateMedia(@RequestBody MediaRequest mediaRequest) {
        log.info("Received updateMedia request: {}", mediaRequest.toString());

        updateService.updateMedia(mediaRequest.getPath());

        log.info("Media updated successfully.");
    }
}

package com.github.rahmnathan.localmovie.web.admin;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.data.MediaRequest;
import com.github.rahmnathan.localmovie.media.MediaUpdateService;
import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping(value = "/admin/v1")
public class MediaAdminResource {
    private final MediaUpdateService updateService;
    private final ServiceConfig serviceConfig;

    @PostMapping(path = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateMedia(@RequestBody MediaRequest mediaRequest) throws InvalidMediaException {
        log.info("Received updateMedia request: {}", mediaRequest.toString());

        updateService.updateMedia(mediaRequest.getPath());

        log.info("Media updated successfully.");
    }

    @PutMapping(path = "/config/conversion-service")
    public void toggleConversionService(@RequestParam boolean enabled) {
        log.info("Setting conversion service enabled to: {}", enabled);
        serviceConfig.getConversionService().setEnabled(enabled);
    }
}

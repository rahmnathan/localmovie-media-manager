package com.github.rahmnathan.localmovie.web.webapp;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import io.micrometer.core.instrument.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@AllArgsConstructor
public class MediaStreamingService {
    private final MeterRegistry registry;

    public ResponseEntity<Resource> streamMediaFile(MediaFile mediaFile, HttpServletRequest request, HttpServletResponse response) {
        if (response == null || request == null)
            return null;

        Path file = Paths.get(mediaFile.getAbsolutePath());

        String mediaName = mediaFile.getPath().replaceAll("[/.]", "-");
        registry.summary("localmovies.streams.requests", "media_name", mediaName).record(1);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(file));
    }
}
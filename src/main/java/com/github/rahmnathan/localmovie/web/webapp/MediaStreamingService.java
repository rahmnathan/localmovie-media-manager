package com.github.rahmnathan.localmovie.web.webapp;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import io.micrometer.core.instrument.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@AllArgsConstructor
public class MediaStreamingService {
    private final Map<String, AtomicInteger> activeStreams = new ConcurrentHashMap<>();

    private final MeterRegistry registry;

    public ResponseEntity<Resource> streamMediaFile(MediaFile mediaFile, HttpServletRequest request, HttpServletResponse response) {
        if (response == null || request == null)
            return null;

        Path file = Paths.get(mediaFile.getAbsolutePath());

        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // TODO - figure out how to make this work
    private AtomicInteger getGauge(String path) {
        String jobId = path.replaceAll("[/.]", "-");

        return activeStreams.computeIfAbsent(jobId, k -> {
            Gauge.builder("localmovies.streams.active", activeStreams, map -> map.get(jobId).doubleValue())
                    .tags(Tags.of("job_id", jobId))
                    .register(registry);

            return new AtomicInteger(0);
        });
    }
}
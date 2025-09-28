package com.github.rahmnathan.localmovie.web.webapp;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import io.micrometer.core.instrument.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class MediaStreamingService {
    private static final long CHUNK_SIZE = 1024 * 1024 * 4;
    private final MeterRegistry registry;

    public ResponseEntity<ResourceRegion> streamMediaFile(MediaFile mediaFile, HttpHeaders requestHeaders) {
        String mediaName = mediaFile.getPath().replaceAll("[/.]", "-");
        registry.summary("localmovies.streams.requests", "media_name", mediaName).record(1);

        FileSystemResource videoResource = new FileSystemResource(Paths.get(mediaFile.getAbsolutePath()));
        long contentLength = 0L;
        try {
            contentLength = videoResource.contentLength();
        } catch (IOException e) {
            log.error("Failed to load content-length.", e);
        }

        ResourceRegion region = getResourceRegion(requestHeaders, contentLength, videoResource);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaTypeFactory.getMediaType(videoResource).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(region);
    }

    private ResourceRegion getResourceRegion(HttpHeaders requestHeaders, long contentLength, FileSystemResource videoResource) {
        List<HttpRange> ranges = requestHeaders.getRange();
        ResourceRegion region;

        if (ranges.isEmpty()) {
            // No range requested, send the first chunk
            long rangeLength = Math.min(CHUNK_SIZE, contentLength);
            region = new ResourceRegion(videoResource, 0, rangeLength);
        } else {
            // Handle requested range
            HttpRange range = ranges.getFirst();
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(CHUNK_SIZE, end - start + 1);
            region = new ResourceRegion(videoResource, start, rangeLength);
        }
        return region;
    }
}
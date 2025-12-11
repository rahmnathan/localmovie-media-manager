package com.github.rahmnathan.localmovie.web.webapp;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import io.micrometer.core.instrument.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class MediaStreamingService {
    private static final long CHUNK_SIZE = 1024 * 1024 * 4;
    private final MeterRegistry registry;
    private final ClientCapabilityService clientCapabilityService;
    private final TranscodedStreamingService transcodedStreamingService;

    public ResponseEntity<StreamingResponseBody> streamMediaFile(MediaFile mediaFile, HttpHeaders requestHeaders) {
        String mediaName = mediaFile.getPath().replaceAll("[/.]", "-");
        registry.summary("localmovies.streams.requests", "media_name", mediaName).record(1);

        // Detect client capabilities
        ClientCapabilities capabilities = clientCapabilityService.detectCapabilities(requestHeaders);

        // Check if client supports the current format
        boolean clientSupportsFormat = capabilities.supports(
            mediaFile.getVideoCodec(),
            mediaFile.getAudioCodec(),
            mediaFile.getContainerFormat()
        );

        if (clientSupportsFormat) {
            log.debug("Client supports format for: {} (video: {}, audio: {}, container: {}) - streaming directly",
                mediaFile.getPath(),
                mediaFile.getVideoCodec(),
                mediaFile.getAudioCodec(),
                mediaFile.getContainerFormat());
            return streamDirectFile(mediaFile, requestHeaders);
        }

        // Client doesn't support current format - select best target codec
        String targetVideoCodec = clientCapabilityService.selectBestVideoCodec(
            capabilities,
            mediaFile.getVideoCodec()
        );
        String targetAudioCodec = clientCapabilityService.selectBestAudioCodec(
            capabilities,
            mediaFile.getAudioCodec()
        );

        log.info("Client does not support format for: {} - transcoding (current: video={}, audio={}, container={}) -> (target: video={}, audio={})",
            mediaFile.getPath(),
            mediaFile.getVideoCodec(),
            mediaFile.getAudioCodec(),
            mediaFile.getContainerFormat(),
            targetVideoCodec,
            targetAudioCodec);

        return transcodedStreamingService.streamTranscodedVideo(
            mediaFile,
            requestHeaders,
            targetVideoCodec,
            targetAudioCodec
        );
    }

    private ResponseEntity<StreamingResponseBody> streamDirectFile(MediaFile mediaFile, HttpHeaders requestHeaders) {
        FileSystemResource videoResource = new FileSystemResource(Paths.get(mediaFile.getAbsolutePath()));

        long contentLength = 0L;
        try {
            contentLength = videoResource.contentLength();
        } catch (IOException e) {
            log.error("Failed to load content-length.", e);
        }

        // Parse range header
        List<HttpRange> ranges = requestHeaders.getRange();
        long start = 0;
        long end = contentLength - 1;

        if (!ranges.isEmpty()) {
            HttpRange range = ranges.getFirst();
            start = range.getRangeStart(contentLength);
            end = range.getRangeEnd(contentLength);
        }

        final long finalStart = start;
        final long finalEnd = end;
        final long finalContentLength = contentLength;

        StreamingResponseBody responseBody = outputStream -> {
            try (InputStream inputStream = videoResource.getInputStream()) {
                // Skip to start position
                inputStream.skip(finalStart);

                long bytesToRead = finalEnd - finalStart + 1;
                byte[] buffer = new byte[8192];
                long totalRead = 0;

                while (totalRead < bytesToRead) {
                    int toRead = (int) Math.min(buffer.length, bytesToRead - totalRead);
                    int bytesRead = inputStream.read(buffer, 0, toRead);

                    if (bytesRead == -1) {
                        break;
                    }

                    outputStream.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }
            }
        };

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaTypeFactory.getMediaType(videoResource).orElse(MediaType.APPLICATION_OCTET_STREAM));
        responseHeaders.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        responseHeaders.setContentLength(finalEnd - finalStart + 1);
        responseHeaders.set(HttpHeaders.CONTENT_RANGE,
            String.format("bytes %d-%d/%d", finalStart, finalEnd, finalContentLength));

        return ResponseEntity
                .status(ranges.isEmpty() ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT)
                .headers(responseHeaders)
                .body(responseBody);
    }
}
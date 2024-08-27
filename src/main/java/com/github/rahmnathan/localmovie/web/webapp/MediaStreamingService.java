package com.github.rahmnathan.localmovie.web.webapp;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import io.micrometer.core.instrument.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
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

    public void streamMediaFile(MediaFile mediaFile, HttpServletRequest request, HttpServletResponse response) {
        if (response == null || request == null)
            return;

        Path file = Paths.get(mediaFile.getAbsolutePath());

        long totalBytes = 0L;
        try {
            totalBytes = Files.size(file);
        } catch (IOException e) {
            log.error("Failure loading file size", e);
        }

        long startByte;
        String rangeHeader = request.getHeader(HttpHeaders.RANGE);
        if (rangeHeader != null) {
            startByte = Long.parseLong(rangeHeader.split("-")[0].substring(6));
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        } else {
            startByte = 0L;
        }

        response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + startByte + "-" + (totalBytes - 1) + "/" + totalBytes);
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(totalBytes - startByte));

        streamFile(file, response, startByte, getGauge(mediaFile.getPath()));
    }

    private void streamFile(Path file, HttpServletResponse response, long startByte, AtomicInteger streamGauge) {
        streamGauge.getAndIncrement();

        try (InputStream input = new BufferedInputStream(Files.newInputStream(file));
             OutputStream output = response.getOutputStream()) {
            skip(input, startByte);
            input.transferTo(output);
        } catch (IOException e) {
            log.error("Failure streaming file", e);
        } finally {
            streamGauge.getAndDecrement();
        }
    }

    private void skip(InputStream inputStream, long amount) throws IOException {
        log.info("Skipping first {} bytes.", amount);
        long skipped = inputStream.skip(amount);
        if (skipped < amount) {
            skip(inputStream, amount - skipped);
        }
    }

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
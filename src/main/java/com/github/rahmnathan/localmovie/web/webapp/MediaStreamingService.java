package com.github.rahmnathan.localmovie.web.webapp;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import io.micrometer.core.annotation.Timed;
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
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class MediaStreamingService {
    private final MeterRegistry registry;

    @Timed(value = "media_stream", longTask = true)
    public void streamMediaFile(MediaFile path, HttpServletRequest request, HttpServletResponse response) {
        if (response == null || request == null)
            return;

        Path file = Paths.get(path.getAbsolutePath());

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

        String jobId = path.getPath().replaceAll("[/.]", "-");

        registry.timer("localmovies.streams.active", List.of(Tag.of("jobId", jobId)))
                .record(() -> streamFile(file, response, startByte));
    }

    private void streamFile(Path file, HttpServletResponse response, long startByte) {
        try (InputStream input = new BufferedInputStream(Files.newInputStream(file));
             OutputStream output = response.getOutputStream()) {

            skip(input, startByte);
            input.transferTo(output);
        } catch (IOException e){
            log.error("Failure streaming file", e);
        }
    }

    private void skip(InputStream inputStream, long amount) throws IOException {
        log.info("Skipping first {} bytes.", amount);
        long skipped = inputStream.skip(amount);
        if(skipped < amount) {
            skip(inputStream, amount - skipped);
        }
    }
}
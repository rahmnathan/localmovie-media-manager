package com.github.rahmnathan.localmovie.web;

import com.github.rahmnathan.localmovie.persistence.control.MediaPersistenceService;
import io.micrometer.core.instrument.Metrics;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FileSenderService {

    private final Logger logger = LoggerFactory.getLogger(FileSenderService.class.getName());
    private final AtomicInteger activeStreamGauge = Metrics.gauge("localmovies.stream.active", new AtomicInteger(0));
    private static final int DEFAULT_BUFFER_SIZE = 16384;
    private final MediaPersistenceService persistenceService;

    public FileSenderService(MediaPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    public void serveResource(Path file, HttpServletRequest request, HttpServletResponse response) {
        if (response == null || request == null || file == null)
            return;

        long totalBytes = 0L;
        try {
            totalBytes = Files.size(file);
        } catch (IOException e) {
            logger.error("Failure loading file size", e);
        }

        long startByte = 0L;
        String rangeHeader = request.getHeader("Range");
        if (rangeHeader != null) {
            startByte = Long.parseLong(rangeHeader.split("-")[0].substring(6));
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        }

        response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + startByte + "-" + (totalBytes - 1) + "/" + totalBytes);
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(totalBytes - startByte));

        streamFile(file, response, startByte);
    }

    private void streamFile(Path file, HttpServletResponse response, long startByte){
        long currentPosition = startByte;
        try (InputStream input = new BufferedInputStream(Files.newInputStream(file));
             OutputStream output = response.getOutputStream()) {

            activeStreamGauge.getAndIncrement();

            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            input.skip(startByte);
            while (true) {
                int read = input.read(buffer);
                if(read < 1) break;
                currentPosition += read;

                output.write(buffer);
                output.flush();
            }

        } catch (IOException e) {
            logger.info("Client stopped stream.");
        } finally {
            activeStreamGauge.getAndDecrement();
            persistenceService.addView(file.toFile().getAbsolutePath(), currentPosition);
        }
    }
}
package com.github.rahmnathan.localmovie.web;

import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import io.micrometer.core.instrument.Metrics;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@AllArgsConstructor
public class FileSenderService {
    private final AtomicInteger activeStreamGauge = Metrics.gauge("localmovies.stream.active", new AtomicInteger(0));
    private final FileSenderHeaderService fileSenderHeaderService;

    public void streamMediaFile(MediaFile mediaFile, HttpServletRequest request, HttpServletResponse response) {
        if (response == null || request == null)
            return;

        Path file = Paths.get(mediaFile.getAbsolutePath());
        long fileSize = getFileSize(file);
        long startByte = fileSenderHeaderService.parseStartByte(request);

        fileSenderHeaderService.setResponseHeaders(fileSize, startByte, response);

        streamFile(file, response, startByte);
    }

    private long getFileSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            log.error("Failure loading file size", e);
            return 0L;
        }
    }

    private void streamFile(Path file, HttpServletResponse response, long startByte){
        try (InputStream input = new BufferedInputStream(Files.newInputStream(file));
             OutputStream output = response.getOutputStream()) {

            activeStreamGauge.getAndIncrement();

            skip(input, startByte);
            input.transferTo(output);
        } catch (IOException e) {
            log.info("Client stopped stream.");
        } finally {
            activeStreamGauge.getAndDecrement();
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
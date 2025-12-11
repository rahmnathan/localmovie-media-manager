package com.github.rahmnathan.localmovie.web.webapp;

import com.github.rahmnathan.localmovie.media.exception.TranscodingException;
import com.github.rahmnathan.localmovie.media.transcoding.FFmpegTranscodingService;
import com.github.rahmnathan.localmovie.media.transcoding.TranscodingProcessManager;
import com.github.rahmnathan.localmovie.persistence.entity.MediaFile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class TranscodedStreamingService {
    private final FFmpegTranscodingService transcodingService;
    private final TranscodingProcessManager processManager;

    public ResponseEntity<StreamingResponseBody> streamTranscodedVideo(
            MediaFile mediaFile,
            HttpHeaders requestHeaders,
            String targetVideoCodec,
            String targetAudioCodec) {

        String sessionId = UUID.randomUUID().toString();
        long startPositionSeconds = extractStartPosition(requestHeaders, mediaFile);

        log.info("Starting transcoded stream for: {} (session: {}, start: {}s, target: video={}, audio={})",
            mediaFile.getPath(), sessionId, startPositionSeconds, targetVideoCodec, targetAudioCodec);

        StreamingResponseBody responseBody = outputStream -> {
            Process ffmpegProcess = null;
            try {
                // Acquire transcoding process
                ffmpegProcess = processManager.acquireTranscodingProcess(
                    sessionId,
                    () -> {
                        try {
                            return transcodingService.startTranscodingProcess(
                                mediaFile.getAbsolutePath(),
                                startPositionSeconds,
                                targetVideoCodec,
                                targetAudioCodec
                            );
                        } catch (TranscodingException e) {
                            throw new RuntimeException("Failed to start transcoding", e);
                        }
                    }
                );

                // Stream FFmpeg output to HTTP response
                try (InputStream processOutput = ffmpegProcess.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;

                    while ((bytesRead = processOutput.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        outputStream.flush();
                    }
                }

                // Check if process completed successfully
                int exitCode = ffmpegProcess.waitFor();
                if (exitCode != 0) {
                    log.error("FFmpeg process failed with exit code: {} for session: {}", exitCode, sessionId);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Transcoding interrupted for session: {}", sessionId, e);
            } catch (IOException e) {
                log.error("I/O error during transcoding for session: {}", sessionId, e);
                throw e;
            } catch (Exception e) {
                log.error("Unexpected error during transcoding for session: {}", sessionId, e);
                throw new IOException("Transcoding failed", e);
            } finally {
                processManager.releaseProcess(sessionId);
            }
        };

        // Determine content type based on codecs
        String contentType = getContentType(targetVideoCodec, targetAudioCodec);

        return ResponseEntity.ok()
            .header(HttpHeaders.ACCEPT_RANGES, "none") // Disable range requests for transcoded content
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .header("X-Transcoded", "true") // Debugging header
            .header("X-Session-Id", sessionId)
            .header("X-Target-Video-Codec", targetVideoCodec)
            .header("X-Target-Audio-Codec", targetAudioCodec)
            .body(responseBody);
    }

    private String getContentType(String videoCodec, String audioCodec) {
        // VP8, VP9, AV1 with Opus/Vorbis -> WebM
        if (videoCodec.matches("(?i)(vp8|vp9|av1)") &&
            audioCodec.matches("(?i)(opus|vorbis)")) {
            return "video/webm";
        }
        // Default to MP4 for H.264, HEVC
        return "video/mp4";
    }

    private long extractStartPosition(HttpHeaders headers, MediaFile mediaFile) {
        List<HttpRange> ranges = headers.getRange();

        if (ranges == null || ranges.isEmpty()) {
            return 0; // Start from beginning
        }

        // Extract byte position from range header
        HttpRange range = ranges.get(0);
        long bytePosition = range.getRangeStart(Long.MAX_VALUE);

        if (bytePosition <= 0) {
            return 0;
        }

        // Convert byte position to time position using bitrate
        // This is an approximation - more accurate seeking would require analyzing the file
        if (mediaFile.getBitrateKbps() != null && mediaFile.getBitrateKbps() > 0) {
            // bitrate is in kbps, convert to bytes per second
            long bytesPerSecond = mediaFile.getBitrateKbps() * 1000L / 8L;
            long seconds = bytePosition / bytesPerSecond;

            log.debug("Converted byte position {} to {}s using bitrate {}kbps",
                bytePosition, seconds, mediaFile.getBitrateKbps());

            return seconds;
        }

        // Fallback: no bitrate available, start from beginning
        log.debug("No bitrate available for seek conversion, starting from beginning");
        return 0;
    }
}

package com.github.rahmnathan.localmovie.media.ffprobe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rahmnathan.localmovie.config.ServiceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FFprobeService {
    private final String ffprobePath;
    private final ObjectMapper objectMapper;

    public FFprobeService(ServiceConfig serviceConfig, ObjectMapper objectMapper) {
        this.ffprobePath = serviceConfig.getDirectoryMonitor().getFfprobeLocation();
        this.objectMapper = objectMapper;
    }

    public VideoMetadata analyzeVideo(String filePath) throws IOException {
        if (!Files.exists(Paths.get(filePath))) {
            throw new IOException("Video file not found: " + filePath);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
            ffprobePath,
            "-v", "quiet",
            "-print_format", "json",
            "-show_format",
            "-show_streams",
            filePath
        );

        Process process;
        try {
            process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("FFprobe analysis timed out for: " + filePath);
            }

            if (process.exitValue() != 0) {
                throw new IOException("FFprobe failed with exit code: " + process.exitValue());
            }

            return parseFFprobeOutput(output.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("FFprobe analysis interrupted", e);
        }
    }

    private VideoMetadata parseFFprobeOutput(String jsonOutput) throws IOException {
        JsonNode root = objectMapper.readTree(jsonOutput);

        String videoCodec = null;
        String audioCodec = null;
        String resolution = null;

        // Parse streams
        JsonNode streams = root.get("streams");
        if (streams != null && streams.isArray()) {
            for (JsonNode stream : streams) {
                String codecType = stream.has("codec_type") ? stream.get("codec_type").asText() : null;

                if ("video".equals(codecType) && videoCodec == null) {
                    videoCodec = stream.has("codec_name") ? stream.get("codec_name").asText() : null;

                    // Get resolution
                    if (stream.has("width") && stream.has("height")) {
                        int width = stream.get("width").asInt();
                        int height = stream.get("height").asInt();
                        resolution = width + "x" + height;
                    }
                } else if ("audio".equals(codecType) && audioCodec == null) {
                    audioCodec = stream.has("codec_name") ? stream.get("codec_name").asText() : null;
                }
            }
        }

        // Parse format
        JsonNode format = root.get("format");
        String containerFormat = null;
        Long durationSeconds = null;
        Integer bitrateKbps = null;

        if (format != null) {
            if (format.has("format_name")) {
                containerFormat = format.get("format_name").asText().split(",")[0]; // Take first format
            }

            if (format.has("duration")) {
                double duration = format.get("duration").asDouble();
                durationSeconds = (long) duration;
            }

            if (format.has("bit_rate")) {
                long bitrate = format.get("bit_rate").asLong();
                bitrateKbps = (int) (bitrate / 1000); // Convert to kbps
            }
        }

        return VideoMetadata.builder()
            .videoCodec(videoCodec)
            .audioCodec(audioCodec)
            .containerFormat(containerFormat)
            .durationSeconds(durationSeconds)
            .bitrateKbps(bitrateKbps)
            .resolution(resolution)
            .build();
    }
}

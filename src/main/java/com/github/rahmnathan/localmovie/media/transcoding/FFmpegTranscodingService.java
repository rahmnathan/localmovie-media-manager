package com.github.rahmnathan.localmovie.media.transcoding;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.media.exception.TranscodingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FFmpegTranscodingService {
    private final String ffmpegPath;
    private final String encodingPreset;
    private final int crf;

    public FFmpegTranscodingService(ServiceConfig serviceConfig) {
        ServiceConfig.TranscodingConfig config = serviceConfig.getTranscoding();
        this.ffmpegPath = config.getFfmpegLocation();
        this.encodingPreset = config.getEncodingPreset();
        this.crf = config.getCrf();
    }

    public Process startTranscodingProcess(
            String inputPath,
            long startPositionSeconds,
            String targetVideoCodec,
            String targetAudioCodec) throws TranscodingException {
        if (!Files.exists(Paths.get(inputPath))) {
            throw new TranscodingException("Input file not found: " + inputPath);
        }

        List<String> command = buildFFmpegCommand(inputPath, startPositionSeconds, targetVideoCodec, targetAudioCodec);

        log.info("Starting FFmpeg transcoding process for: {} (start position: {}s, target: video={}, audio={})",
            inputPath, startPositionSeconds, targetVideoCodec, targetAudioCodec);
        log.debug("FFmpeg command: {}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(false); // Keep stderr separate for logging

        try {
            Process process = processBuilder.start();

            // Log stderr in a separate thread for debugging
            Thread stderrLogger = new Thread(() -> {
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.debug("FFmpeg stderr: {}", line);
                    }
                } catch (IOException e) {
                    log.warn("Error reading FFmpeg stderr", e);
                }
            });
            stderrLogger.setDaemon(true);
            stderrLogger.start();

            return process;
        } catch (IOException e) {
            throw new TranscodingException("Failed to start FFmpeg process", e);
        }
    }

    private List<String> buildFFmpegCommand(
            String inputPath,
            long startPositionSeconds,
            String targetVideoCodec,
            String targetAudioCodec) {
        List<String> command = new ArrayList<>();

        command.add(ffmpegPath);

        // Seek to start position (input seeking is faster than output seeking)
        if (startPositionSeconds > 0) {
            command.add("-ss");
            command.add(String.valueOf(startPositionSeconds));
        }

        // Input file
        command.add("-i");
        command.add(inputPath);

        // Video encoding
        command.add("-c:v");
        command.add(getVideoEncoderName(targetVideoCodec));
        command.add("-preset");
        command.add(encodingPreset);
        command.add("-crf");
        command.add(String.valueOf(crf));

        // Audio encoding
        command.add("-c:a");
        command.add(getAudioEncoderName(targetAudioCodec));
        if ("aac".equalsIgnoreCase(targetAudioCodec)) {
            command.add("-b:a");
            command.add("192k"); // 192 kbps audio bitrate for AAC
        }

        // Container format and streaming flags
        String outputFormat = getOutputFormat(targetVideoCodec, targetAudioCodec);
        if ("mp4".equals(outputFormat)) {
            // MP4 streaming flags
            command.add("-movflags");
            command.add("frag_keyframe+empty_moov+faststart");
        }

        // Output format
        command.add("-f");
        command.add(outputFormat);

        // Output to stdout
        command.add("pipe:1");

        return command;
    }

    private String getVideoEncoderName(String codec) {
        return switch (codec.toLowerCase()) {
            case "h264", "avc" -> "libx264";
            case "h265", "hevc" -> "libx265";
            case "vp8" -> "libvpx";
            case "vp9" -> "libvpx-vp9";
            case "av1" -> "libaom-av1";
            default -> "libx264"; // Fallback to H.264
        };
    }

    private String getAudioEncoderName(String codec) {
        return switch (codec.toLowerCase()) {
            case "aac" -> "aac";
            case "mp3" -> "libmp3lame";
            case "opus" -> "libopus";
            case "vorbis" -> "libvorbis";
            default -> "aac"; // Fallback to AAC
        };
    }

    private String getOutputFormat(String videoCodec, String audioCodec) {
        // VP8, VP9, AV1 with Opus/Vorbis -> WebM
        if (videoCodec.matches("(?i)(vp8|vp9|av1)") &&
            audioCodec.matches("(?i)(opus|vorbis)")) {
            return "webm";
        }
        // Default to MP4 for H.264, HEVC
        return "mp4";
    }
}

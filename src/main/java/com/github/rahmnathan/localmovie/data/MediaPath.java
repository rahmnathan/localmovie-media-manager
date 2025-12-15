package com.github.rahmnathan.localmovie.data;

import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;
import com.github.rahmnathan.omdb.data.MediaType;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;


@Slf4j
@Getter
@Builder
public class MediaPath {
    public static final String MEDIA_ROOT_FOLDER = "LocalMedia" + File.separator;
    public static final String MOVIES_FOLDER = "Movies" + File.separator;
    public static final String SERIES_FOLDER = "Series" + File.separator;

    private final String relativePath;
    private final MediaType mediaType;
    private final String fileName;
    private final String title;
    private final String absolutePath;
    private final String destinationPath;
    private final boolean ignore;
    private final boolean streamable;
    private final MediaPath seriesPath;
    private final MediaPath parentPath;

    // For series only
    private final Integer seasonNumber;
    private final Integer episodeNumber;

    public String asJobId() {
        return relativePath.replaceAll("[^A-Za-z0-9]", "-");
    }

    @Override
    public String toString() {
        return StringUtils.isBlank(absolutePath) ? relativePath : absolutePath;
    }

    public static MediaPath parse(File file) throws InvalidMediaException {
        return parse(file.getAbsolutePath());
    }

    public static MediaPath safeParse(String path) {
        try {
            return parse(path);
        } catch (InvalidMediaException e) {
            log.error("Failed to parse media path: {}", path, e);
            return null;
        }
    }

    public static MediaPath parse(String path) throws InvalidMediaException {
        MediaPathBuilder builder = MediaPath.builder();

        if (path.contains(MEDIA_ROOT_FOLDER)) {
            builder.absolutePath(path);
            builder.relativePath(path.split(MEDIA_ROOT_FOLDER)[1]);
            builder.destinationPath(getConversionOutputPath(builder.absolutePath));
        } else {
            builder.relativePath(path);
        }

        MediaFileType mediaFileType = MediaFileType.parse(builder.relativePath).orElseThrow(() -> new InvalidMediaException("Invalid media path: " + path));
        mediaFileType.extractPathElements(path, builder);

        return builder.build();
    }

    private static String getConversionOutputPath(String absolutePath) {
        if (absolutePath.length() > 4 && absolutePath.charAt(absolutePath.length() - 4) == '.') {
            return absolutePath.substring(0, absolutePath.lastIndexOf('.')) + (absolutePath.endsWith(".mp4") ? ".mkv" : ".mp4");
        }

        return null;
    }
}

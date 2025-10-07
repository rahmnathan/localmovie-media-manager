package com.github.rahmnathan.localmovie.data;

import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;
import com.github.rahmnathan.omdb.data.MediaType;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

    // For series only
    private final MediaPath seriesPath;
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

    public static MediaPath parse(String path) throws InvalidMediaException {
        MediaPathBuilder builder = MediaPath.builder();

        if (path.contains(MEDIA_ROOT_FOLDER)) {
            builder.absolutePath(path);
            builder.relativePath(path.split(MEDIA_ROOT_FOLDER)[1]);
            builder.destinationPath(getConversionOutputPath(builder.absolutePath));
        } else {
            builder.relativePath(path);
        }

        MediaType mediaType = getMediaType(builder.relativePath);
        builder.mediaType(mediaType);

        if (mediaType == MediaType.EPISODE || mediaType == MediaType.SEASON) {
            builder.seriesPath(getSeriesPath(builder.relativePath));
            builder.seasonNumber(parseSeasonNumber(builder.relativePath));

            if (mediaType == MediaType.EPISODE) {
                builder.episodeNumber(parseEpisodeNumber(builder.relativePath));
            }
        }

        builder.fileName(getFileName(builder.relativePath));
        builder.title(getTitle(builder.fileName));

        return builder.build();
    }

    private static String getConversionOutputPath(String absolutePath) {
        if (absolutePath.length() > 4 && absolutePath.charAt(absolutePath.length() - 4) == '.') {
            return absolutePath.substring(0, absolutePath.lastIndexOf('.')) + (absolutePath.endsWith(".mp4") ? ".mkv" : ".mp4");
        }

        return null;
    }

    private static String getFileName(String path) {
        return new File(path).getName();
    }

    private static String getTitle(String fileName) {
        if (fileName.length() > 4 && fileName.charAt(fileName.length() - 4) == '.') {
            return fileName.substring(0, fileName.length() - 4);
        }

        return fileName;
    }


    private static MediaType getMediaType(String path) throws InvalidMediaException {
        if (isEpisode(path)) {
            return MediaType.EPISODE;
        }
        if (isSeason(path)) {
            return MediaType.SEASON;
        }
        if (isSeries(path)) {
            return MediaType.SERIES;
        }
        if (isMovie(path)) {
            return MediaType.MOVIE;
        }

        throw new InvalidMediaException("Unsupported path: " + path);
    }

    private static boolean isMovie(String path) {
        return isTopLevel(path) && path.startsWith(MOVIES_FOLDER);
    }

    private static boolean isSeries(String path) {
        return isTopLevel(path) && path.startsWith(SERIES_FOLDER);
    }

    private static boolean isTopLevel(String path) {
        return getPathLength(path) == 2;
    }

    private static boolean isSeason(String path) {
        return getPathLength(path) == 3;
    }

    private static boolean isEpisode(String path) {
        return getPathLength(path) == 4;
    }

    private static int getPathLength(String path) {
        return path.split(Pattern.quote(File.separator)).length;
    }

    private static MediaPath getSeriesPath(String path) throws InvalidMediaException {
        String relativePath = SERIES_FOLDER;
        String absolutePrefix = "";

        if (path.contains(SERIES_FOLDER)) {
            if (path.startsWith(SERIES_FOLDER)) {
                relativePath += path.split(Pattern.quote(File.separator))[1];
            } else {
                absolutePrefix = path.substring(0, path.indexOf(SERIES_FOLDER));
                relativePath += path.split(SERIES_FOLDER)[1].split(Pattern.quote(File.separator))[0];
            }
        } else {
            throw new InvalidMediaException(path + " does not contain series folder. Expected to contain: 'Series/'");
        }

        return MediaPath.parse(absolutePrefix + relativePath);
    }

    private static int parseEpisodeNumber(String path) throws InvalidMediaException {
        return parseNumber(NumberParser.EPISODE, path);
    }

    private static int parseSeasonNumber(String path) throws InvalidMediaException {
        return parseNumber(NumberParser.SEASON, path);
    }

    private static int parseNumber(NumberParser numberParser, String path) throws InvalidMediaException {
        return numberParser.getPatterns().stream()
                .map(regex -> Pattern.compile(regex).matcher(path))
                .filter(Matcher::find)
                .map(matcher -> Integer.parseInt(matcher.group()))
                .findAny()
                .orElseThrow(() -> new InvalidMediaException("Unable to parse number from String: " + path));
    }

    private enum NumberParser {
        SEASON("(?<=Season )\\d+"),
        EPISODE("(?<=Episode )\\d+", "(?<=S\\d\\dE)\\d+");

        private final String[] patterns;

        NumberParser(String... patterns) {
            this.patterns = patterns;
        }

        private Set<String> getPatterns() {
            return Set.of(patterns);
        }
    }
}

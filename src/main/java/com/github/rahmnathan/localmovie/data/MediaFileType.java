package com.github.rahmnathan.localmovie.data;

import com.github.rahmnathan.omdb.data.MediaType;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

@Getter
public enum MediaFileType {
    MOVIE(MediaType.MOVIE, false, true,
            Pattern.compile("^Movies/.*\\.[A-Za-z0-9]+$"),
            MediaPathElement.MOVIE_NAME,
            MediaPathElement.FILE_WITH_EXTENSION
    ),
    MOVIE_FOLDER(null, true, false,
            Pattern.compile("^Movies/(?!.*\\.[A-Za-z0-9]+$)[^/]+$"),
            MediaPathElement.FILE_WITH_EXTENSION
    ),
    SERIES(MediaType.SERIES, false, false,
            Pattern.compile("^Series/[^/]+$"),
            MediaPathElement.SERIES_NAME,
            MediaPathElement.FILE_WITH_EXTENSION
    ),
    SEASON(MediaType.SEASON, false, false,
            Pattern.compile("^Series/[^/]+/Season [0-9]+$"),
            MediaPathElement.SEASON_NAME,
            MediaPathElement.SERIES_PATH,
            MediaPathElement.SERIES_PATH_PARENT,
            MediaPathElement.SEASON_NUMBER,
            MediaPathElement.FILE_WITH_EXTENSION
    ),
    EPISODE(MediaType.EPISODE, false, true,
            Pattern.compile("^Series/[^/]+/Season [0-9]+/[^/]+\\.[A-Za-z0-9]+$"),
            MediaPathElement.EPISODE_NAME,
            MediaPathElement.SERIES_PATH,
            MediaPathElement.SEASON_PATH_PARENT,
            MediaPathElement.SEASON_NUMBER,
            MediaPathElement.EPISODE_NUMBER,
            MediaPathElement.FILE_WITH_EXTENSION
    );

    private final MediaType mediaType;
    private final boolean ignore;
    private final boolean streamable;
    private final Pattern pathPattern;
    private final MediaPathElement[] pathElements;

    MediaFileType(MediaType mediaType, boolean ignore, boolean streamable, Pattern pathPattern, MediaPathElement... pathElements) {
        this.mediaType = mediaType;
        this.ignore = ignore;
        this.streamable = streamable;
        this.pathPattern = pathPattern;
        this.pathElements = pathElements;
    }

    public static Optional<MediaFileType> parse(String path) {
        return Arrays.stream(MediaFileType.values())
                .filter(mediaFileType -> mediaFileType.getPathPattern().matcher(path).matches())
                .findFirst();
    }

    public void extractPathElements(String path, MediaPath.MediaPathBuilder builder) {
        builder.mediaType(mediaType);
        builder.streamable(streamable);
        builder.ignore(ignore);
        Arrays.stream(pathElements).forEach(extractor -> extractor.apply(path, builder));
    }
}

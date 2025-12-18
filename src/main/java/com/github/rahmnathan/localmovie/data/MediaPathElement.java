package com.github.rahmnathan.localmovie.data;

import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum MediaPathElement {
    SERIES_NAME(
            false,
            String::valueOf,
            MediaPath.MediaPathBuilder::title,
            Pattern.compile("(?<=Series/).+(?= \\([0-9]{4}\\))"),
            Pattern.compile("(?<=Series/)[^/]+")
    ),
    SERIES_PATH(
            false,
            MediaPath::safeParse,
            MediaPath.MediaPathBuilder::seriesPath,
            Pattern.compile(".*Series/[^/]+")
    ),
    RELEASE_YEAR(
            true,
            String::valueOf,
            MediaPath.MediaPathBuilder::releaseYear,
            Pattern.compile("(?<=\\()[0-9]{4}(?=\\))")
    ),
    SERIES_PATH_PARENT(
            false,
            MediaPath::safeParse,
            MediaPath.MediaPathBuilder::parentPath,
            Pattern.compile(".*Series/[^/]+")
    ),
    SEASON_PATH_PARENT(
            false,
            MediaPath::safeParse,
            MediaPath.MediaPathBuilder::parentPath,
            Pattern.compile(".*Series/[^/]+/[^/]+(?=/.*\\.[A-Za-z0-9]+$)")
    ),
    MOVIE_NAME(
            false,
            String::valueOf,
            MediaPath.MediaPathBuilder::title,
            Pattern.compile("(?<=Movies/).+(?= \\([0-9]{4}\\))"),
            Pattern.compile("(?<=Movies/)[^/]+(?=/.*)"),
            Pattern.compile("(?<=Movies/)[^/]+(?=\\.[A-Za-z0-9]+$)")
    ),
    MOVIE_SUBTITLE_NAME(
            false,
            String::valueOf,
            MediaPath.MediaPathBuilder::title,
            Pattern.compile("(?<=Movies/)[^/]+(?=/.*)"),
            Pattern.compile("(?<=Movies/)[^/]+(?=\\.[A-Za-z0-9]+$)")
    ),
    SEASON_NAME(
            false,
            String::valueOf,
            MediaPath.MediaPathBuilder::title,
            Pattern.compile("Season \\d{1,2}")
    ),
    SEASON_NUMBER(
            false,
            Integer::valueOf,
            MediaPath.MediaPathBuilder::seasonNumber,
            Pattern.compile("(?<=Season )\\d+")
    ),
    EPISODE_NAME(
            false,
            String::valueOf,
            MediaPath.MediaPathBuilder::title,
            Pattern.compile("[^/]+(?=\\.[^/.]+$)")
    ),
    EPISODE_SUBTITLE_NAME(
            false,
            String::valueOf,
            MediaPath.MediaPathBuilder::title,
            Pattern.compile("[^/]+(?=\\.[^/.]+$)")
    ),
    EPISODE_NUMBER(
            false,
            Integer::valueOf,
            MediaPath.MediaPathBuilder::episodeNumber,
            Pattern.compile("(?<=Episode )\\d+"),
            Pattern.compile("(?<=S\\d{1,2}E)\\d+")
    ),
    FILE_WITH_EXTENSION(
            false,
            String::valueOf,
            MediaPath.MediaPathBuilder::fileName,
            Pattern.compile("(?<=/)[^/]+$")
    );

    private final boolean optional;
    private final Function<String, ?> parser;
    private final BiConsumer<MediaPath.MediaPathBuilder, ?> setter;
    private final Pattern[] patterns;

    <T> MediaPathElement(boolean optional, Function<String, T> parser, BiConsumer<MediaPath.MediaPathBuilder, T> setter, Pattern... patterns) {
        this.optional = optional;
        this.parser = parser;
        this.setter = setter;
        this.patterns = patterns;
    }

    @SuppressWarnings("unchecked")
    public void apply(String path, MediaPath.MediaPathBuilder builder) throws InvalidMediaException {
        Optional<?> value = Arrays.stream(patterns)
                .map(pattern -> pattern.matcher(path))
                .filter(Matcher::find)
                .map(matcher -> parser.apply(matcher.group()))
                .findFirst();

        if (value.isEmpty() && !optional) {
            throw new InvalidMediaException("Failed to extract " + this.name() + " from path: " + path);
        }

        value.ifPresent(o -> ((BiConsumer<MediaPath.MediaPathBuilder, Object>) setter).accept(builder, o));
    }
}

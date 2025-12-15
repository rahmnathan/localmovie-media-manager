package com.github.rahmnathan.localmovie.data;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum MediaPathElement {
    SERIES_NAME(
            String::valueOf,
            MediaPath.MediaPathBuilder::title,
            Pattern.compile("(?<=Series/)[^/]+")
    ),
    SERIES_PATH(
            MediaPath::safeParse,
            MediaPath.MediaPathBuilder::seriesPath,
            Pattern.compile(".*Series/[^/]+")
    ),
    SERIES_PATH_PARENT(
            MediaPath::safeParse,
            MediaPath.MediaPathBuilder::parentPath,
            Pattern.compile(".*Series/[^/]+")
    ),
    SEASON_PATH_PARENT(
            MediaPath::safeParse,
            MediaPath.MediaPathBuilder::parentPath,
            Pattern.compile(".*Series/[^/]+/[^/]+(?=/[^/]+\\.[A-Za-z0-9]+$)")
    ),
    MOVIE_NAME(
            String::valueOf,
            MediaPath.MediaPathBuilder::title,
            Pattern.compile("(?<=Movies/)[^/]+(?=/.*)"),
            Pattern.compile("(?<=Movies/)[^/]+(?=\\.[A-Za-z0-9]+$)")
    ),
    SEASON_NAME(
            String::valueOf,
            MediaPath.MediaPathBuilder::title,
            Pattern.compile("Season \\d{1,2}")
    ),
    SEASON_NUMBER(
            Integer::valueOf,
            MediaPath.MediaPathBuilder::seasonNumber,
            Pattern.compile("(?<=Season )\\d+")
    ),
    EPISODE_NAME(
            String::valueOf,
            MediaPath.MediaPathBuilder::title,
            Pattern.compile("(?<=Season \\d{1,2}/)[^/]+(?=\\.[A-Za-z0-9]+$)")
    ),
    EPISODE_NUMBER(
            Integer::valueOf,
            MediaPath.MediaPathBuilder::episodeNumber,
            Pattern.compile("(?<=Episode )\\d+"),
            Pattern.compile("(?<=S\\d{1,2}E)\\d+")
    ),
    FILE_WITH_EXTENSION(
            String::valueOf,
            MediaPath.MediaPathBuilder::fileName,
            Pattern.compile("(?<=/)[^/]+\\.[A-Za-z0-9]+$")
    );

    private final Function<String, ?> parser;
    private final BiConsumer<MediaPath.MediaPathBuilder, ?> setter;
    private final Pattern[] patterns;

    <T> MediaPathElement(Function<String, T> parser, BiConsumer<MediaPath.MediaPathBuilder, T> setter, Pattern... patterns) {
        this.parser = parser;
        this.setter = setter;
        this.patterns = patterns;
    }

    @SuppressWarnings("unchecked")
    public void apply(String path, MediaPath.MediaPathBuilder builder) {
        Arrays.stream(patterns)
                .map(pattern -> pattern.matcher(path))
                .filter(Matcher::find)
                .map(matcher -> parser.apply(matcher.group()))
                .findFirst()
                .ifPresent(value -> ((BiConsumer<MediaPath.MediaPathBuilder, Object>) setter).accept(builder, value));

    }
}

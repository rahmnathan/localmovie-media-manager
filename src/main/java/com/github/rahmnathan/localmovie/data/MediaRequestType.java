package com.github.rahmnathan.localmovie.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum MediaRequestType {
    MOVIES(MediaFileType.MOVIE),
    SERIES(MediaFileType.SERIES),
    SEASONS(MediaFileType.SEASON),
    EPISODES(MediaFileType.EPISODE),
    HISTORY(null);

    private final MediaFileType type;

    private static final Map<String, MediaRequestType> lookup = new HashMap<>();

    static {
        for (MediaRequestType mediaRequestType : MediaRequestType.values()) {
            lookup.put(mediaRequestType.name().toLowerCase(), mediaRequestType);
        }
    }

    public static Optional<MediaRequestType> lookup(String alias) {
        return Optional.ofNullable(lookup.get(alias != null ? alias.toLowerCase() : null));
    }
}

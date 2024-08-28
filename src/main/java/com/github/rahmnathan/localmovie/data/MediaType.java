package com.github.rahmnathan.localmovie.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum MediaType {
    MOVIES,
    SERIES,
    SEASONS,
    EPISODES,
    HISTORY;

    private static final Map<String, MediaType> lookup = new HashMap<>();

    static {
        for (MediaType mediaType : MediaType.values()) {
            lookup.put(mediaType.name(), mediaType);
            lookup.put(mediaType.name().toLowerCase(), mediaType);
        }
    }

    public static Optional<MediaType> lookup(String alias) {
        return Optional.ofNullable(lookup.get(alias));
    }
}

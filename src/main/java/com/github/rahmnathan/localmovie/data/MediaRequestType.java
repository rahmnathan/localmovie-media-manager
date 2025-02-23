package com.github.rahmnathan.localmovie.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum MediaRequestType {
    MOVIES,
    SERIES,
    SEASONS,
    EPISODES,
    HISTORY;

    private static final Map<String, MediaRequestType> lookup = new HashMap<>();

    static {
        for (MediaRequestType mediaRequestType : MediaRequestType.values()) {
            lookup.put(mediaRequestType.name(), mediaRequestType);
            lookup.put(mediaRequestType.name().toLowerCase(), mediaRequestType);
        }
    }

    public static Optional<MediaRequestType> lookup(String alias) {
        return Optional.ofNullable(lookup.get(alias));
    }
}

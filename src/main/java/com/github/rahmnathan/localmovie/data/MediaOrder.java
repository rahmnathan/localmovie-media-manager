package com.github.rahmnathan.localmovie.data;

import com.github.rahmnathan.localmovie.persistence.entity.QMediaFile;
import com.querydsl.core.types.OrderSpecifier;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum MediaOrder {
    DATE_ADDED("added", QMediaFile.mediaFile.created.desc()),
    RATING("rating", QMediaFile.mediaFile.media.imdbRating.desc()),
    RELEASE_YEAR("year", QMediaFile.mediaFile.media.releaseYear.desc()),
    SEASONS_EPISODES("season-episode", QMediaFile.mediaFile.media.number.asc()),
    TITLE("title", QMediaFile.mediaFile.fileName.asc());

    private final String key;
    private final OrderSpecifier<?> orderSpecifier;

    private static final Map<String, MediaOrder> lookup = new HashMap<>();

    MediaOrder(String key, OrderSpecifier<?> orderSpecifier) {
        this.orderSpecifier = orderSpecifier;
        this.key = key;
    }

    static {
        for (MediaOrder mediaOrder : MediaOrder.values()) {
            lookup.put(mediaOrder.getKey(), mediaOrder);
        }
    }

    public static MediaOrder lookup(String key) {
        return lookup.getOrDefault(key, MediaOrder.TITLE);
    }
}

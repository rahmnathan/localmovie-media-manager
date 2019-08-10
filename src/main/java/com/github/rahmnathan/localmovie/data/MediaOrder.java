package com.github.rahmnathan.localmovie.data;

import org.springframework.data.domain.Sort;

public enum MediaOrder {
    DATE_ADDED("created", false),
    MOST_VIEWS("views", false),
    RATING("media.imdbRating", false),
    RELEASE_YEAR("media.releaseYear", false),
    SEASONS_EPISODES("media.number", true),
    TITLE("path", true);

    private final Sort sort;

    MediaOrder(String fieldName, boolean ascending) {
        Sort.Order order = ascending ? Sort.Order.asc(fieldName) : Sort.Order.desc(fieldName);
        this.sort = Sort.by(order);
    }

    public Sort getSort() {
        return sort;
    }
}

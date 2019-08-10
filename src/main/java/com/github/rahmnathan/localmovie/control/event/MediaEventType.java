package com.github.rahmnathan.localmovie.control.event;

public enum MediaEventType {
    ENTRY_CREATE("CREATE"),
    ENTRY_DELETE("DELETE");

    private final String movieEventString;

    MediaEventType(String movieEventString) {
        this.movieEventString = movieEventString;
    }

    public String getMovieEventString() {
        return movieEventString;
    }
}

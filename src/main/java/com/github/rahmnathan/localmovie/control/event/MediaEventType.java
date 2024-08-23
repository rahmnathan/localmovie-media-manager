package com.github.rahmnathan.localmovie.control.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MediaEventType {
    ENTRY_CREATE("CREATE"),
    ENTRY_DELETE("DELETE");

    private final String movieEventString;
}

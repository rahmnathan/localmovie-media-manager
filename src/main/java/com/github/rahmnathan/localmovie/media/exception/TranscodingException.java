package com.github.rahmnathan.localmovie.media.exception;

public class TranscodingException extends Exception {
    public TranscodingException(String message) {
        super(message);
    }

    public TranscodingException(String message, Throwable cause) {
        super(message, cause);
    }
}

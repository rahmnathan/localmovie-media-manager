package com.github.rahmnathan.localmovie.media.omdb;

public class MediaNotFoundException extends MediaProviderException {
    public MediaNotFoundException(String message){
        super(message);
    }
}

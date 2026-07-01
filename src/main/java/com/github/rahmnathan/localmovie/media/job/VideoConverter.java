package com.github.rahmnathan.localmovie.media.job;

import java.io.File;
import java.io.IOException;

public interface VideoConverter {
    void launchVideoConverter(File inputFile, File outputFile) throws IOException;
}

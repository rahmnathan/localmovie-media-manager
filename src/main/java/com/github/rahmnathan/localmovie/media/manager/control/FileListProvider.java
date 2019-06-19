package com.github.rahmnathan.localmovie.media.manager.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FileListProvider {
    private final Logger logger = LoggerFactory.getLogger(FileListProvider.class.getName());
    private final String[] mediaPaths;

    public FileListProvider(@Value("${media.path}") String[] mediaPaths) {
        this.mediaPaths = mediaPaths;
    }

    Set<String> listFiles(String path) {
        logger.info("Listing files at - {}", path);

        Set<String> filePaths = new HashSet<>();
        Arrays.stream(mediaPaths).forEach(mediaPath -> {
            Optional<File[]> fileArray = Optional.ofNullable(new File(mediaPath + path).listFiles());

            fileArray.ifPresent(files -> {
                logger.info("Found {} files.", files.length);
                Set<String> tempFileSet = Arrays.stream(files)
                        .map(file -> file.getAbsolutePath().substring(mediaPath.length()))
                        .collect(Collectors.toSet());

                filePaths.addAll(tempFileSet);
            });
        });

        return filePaths;
    }
}

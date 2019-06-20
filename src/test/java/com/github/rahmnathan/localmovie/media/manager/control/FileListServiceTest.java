package com.github.rahmnathan.localmovie.media.manager.control;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileListServiceTest {

    @Test
    public void listFilesTest(){
        FileListService fileListService = new FileListService(new String[]{File.separator});

        Set<String> fileList = fileListService.listFiles("");
        assertTrue(fileList.size() > 0);
    }
}

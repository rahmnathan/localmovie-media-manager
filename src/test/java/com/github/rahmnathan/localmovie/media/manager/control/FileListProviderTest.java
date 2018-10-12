package com.github.rahmnathan.localmovie.media.manager.control;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileListProviderTest {

    @Test
    public void listFilesTest(){
        FileListProvider fileListProvider = new FileListProvider(new String[]{File.separator});

        Set<String> fileList = fileListProvider.listFiles("");
        assertTrue(fileList.size() > 0);
    }
}

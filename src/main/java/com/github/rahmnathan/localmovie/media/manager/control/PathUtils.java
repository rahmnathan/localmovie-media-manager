package com.github.rahmnathan.localmovie.media.manager.control;

import java.io.File;

class PathUtils {

    private PathUtils(){
        // No need to instantiate this
    }

    static boolean isTopLevel(String currentPath){
        return currentPath.split(File.separator).length == 2;
    }

    static File getParentFile(String path){
        int directoryDepth = path.split(File.separator).length;
        if(!isTopLevel(path))
            directoryDepth -= 2;

        File file = new File(path);
        for(int i = 0; i < directoryDepth; i++){
            file = file.getParentFile();
        }

        return file;
    }

    static String getTitle(String fileName){
        if (fileName.charAt(fileName.length() - 4) == '.') {
            return fileName.substring(0, fileName.length() - 4);
        }

        return fileName;
    }
}

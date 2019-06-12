package com.github.rahmnathan.localmovie.media.manager.control;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PathUtils {

    private PathUtils(){
        // No need to instantiate this
    }

    static boolean isTopLevel(String currentPath){
        int pathLength = currentPath.split(File.separator).length;
        return pathLength == 2;
    }

    static boolean isEpisode(String currentPath){
        int pathLength = currentPath.split(File.separator).length;
        return pathLength == 4;
    }

    static int getEpisodeNumber(String fileName){
        Pattern pattern = Pattern.compile("(?<=Episode )\\d+");
        Matcher matcher = pattern.matcher(fileName);
        return matcher.find() ? Integer.parseInt(matcher.group()) : 1;
    }

    static int getSeasonNumber(String path) {
        Pattern pattern = Pattern.compile("(?<=Season )\\d+");
        Matcher matcher = pattern.matcher(path);
        return matcher.find() ? Integer.parseInt(matcher.group()) : 1;
    }

    static File getParentFile(String path){
        int directoryDepth = path.split(File.separator).length - 2;

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

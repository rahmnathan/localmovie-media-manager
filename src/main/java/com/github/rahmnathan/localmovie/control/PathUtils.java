package com.github.rahmnathan.localmovie.control;

import com.github.rahmnathan.localmovie.exception.InvalidMediaException;

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

    static int getEpisodeNumber(String fileName) throws InvalidMediaException {
        Pattern pattern = Pattern.compile("(?<=Episode )\\d+");
        Matcher matcher = pattern.matcher(fileName);

        if(matcher.find()){
            return Integer.parseInt(matcher.group());
        }

        throw new InvalidMediaException("Unable to parse episode number from String: " + fileName);
    }

    static int getSeasonNumber(String path) throws InvalidMediaException {
        Pattern pattern = Pattern.compile("(?<=Season )\\d+");
        Matcher matcher = pattern.matcher(path);

        if(matcher.find()){
            return Integer.parseInt(matcher.group());
        }

        throw new InvalidMediaException("Unable to parse season number from String: " + path);
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

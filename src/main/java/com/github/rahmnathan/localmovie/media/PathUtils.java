package com.github.rahmnathan.localmovie.media;

import com.github.rahmnathan.localmovie.media.exception.InvalidMediaException;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class PathUtils {
    public static final String LOCAL_MEDIA_FOLDER = "LocalMedia" + File.separator;
    public static final String MOVIES_FOLDER = "Movies" + File.separator;
    public   static final String SERIES_FOLDER = "Series" + File.separator;

    boolean isSeries(String currentPath) {
        return isTopLevel(currentPath) && currentPath.contains(SERIES_FOLDER);
    }

    boolean isTopLevel(String currentPath){
        int pathLength = currentPath.split(Pattern.quote(File.separator)).length;
        return pathLength == 2;
    }

    boolean isEpisode(String currentPath){
        int pathLength = currentPath.split(Pattern.quote(File.separator)).length;
        return pathLength == 4;
    }

    int parseEpisodeNumber(String path) throws InvalidMediaException {
        return parseNumber(NumberParser.EPISODE, path);
    }

    int parseSeasonNumber(String path) throws InvalidMediaException {
        return parseNumber(NumberParser.SEASON, path);
    }

    File getParentFile(String path){
        int directoryDepth = path.split(Pattern.quote(File.separator)).length - 2;

        File file = new File(path);
        for(int i = 0; i < directoryDepth; i++){
            file = file.getParentFile();
        }

        return file;
    }

    String getTitle(String fileName){
        if (fileName.length() > 4 && fileName.charAt(fileName.length() - 4) == '.') {
            return fileName.substring(0, fileName.length() - 4);
        }

        return fileName;
    }

    public String pathToJobId(String path) {
        return path.split(PathUtils.LOCAL_MEDIA_FOLDER)[1].replaceAll("[^A-Za-z0-9]", "-");
    }

    private int parseNumber(NumberParser numberParser, String path) throws InvalidMediaException {
        return numberParser.getPatterns().stream()
                .map(regex -> Pattern.compile(regex).matcher(path))
                .filter(Matcher::find)
                .map(matcher -> Integer.parseInt(matcher.group()))
                .findAny()
                .orElseThrow(() -> new InvalidMediaException("Unable to parse number from String: " + path));
    }

    public enum NumberParser {
        SEASON("(?<=Season )\\d+"),
        EPISODE("(?<=Episode )\\d+", "(?<=S\\d\\dE)\\d+");

        private final String[] patterns;

        NumberParser(String... patterns) {
            this.patterns = patterns;
        }

        public Set<String> getPatterns(){
            return Set.of(patterns);
        }
    }
}

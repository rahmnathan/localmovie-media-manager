package com.github.rahmnathan.localmovie.media.manager.control;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rahmnathan.localmovie.domain.MediaFile;
import com.github.rahmnathan.localmovie.domain.MediaFileEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.rahmnathan.localmovie.domain.CachePrefix.FILE_LIST;
import static com.github.rahmnathan.localmovie.domain.CachePrefix.MEDIA_EVENTS;
import static com.github.rahmnathan.localmovie.domain.CachePrefix.MEDIA_FILE;

@Service
public class MediaCacheService {
    private final Logger logger = LoggerFactory.getLogger(MediaCacheService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Jedis jedis;

    public MediaCacheService(@Value("${jedis.host}") String jedisHost) {
        this.jedis = new Jedis(jedisHost);
    }

    void addMedia(MediaFile mediaFile){
        try {
            logger.info("Adding media file to cache. Key {} Value {}", mediaFile.getPath(), mediaFile);
            jedis.set(MEDIA_FILE + mediaFile.getPath(), OBJECT_MAPPER.writeValueAsString(mediaFile));
        } catch (IOException e){
            logger.error("Failure storing mediaFile in cache.", e);
        }
    }

    private Set<String> listFiles(String path) {
        try {
            return OBJECT_MAPPER.readValue(jedis.get(FILE_LIST + path), Set.class);
        } catch (IOException e){
            logger.error("Failure unmarshalling file list from cache.", e);
            return new HashSet<>();
        }
    }

    void putFiles(String path, Set<String> filePaths){
        try{
            logger.info("Adding files to cache. Key {} Value {}", path, filePaths);
            jedis.set(FILE_LIST + path, OBJECT_MAPPER.writeValueAsString(filePaths));
        } catch (IOException e){
            logger.error("Failure marshalling file paths for cache.", e);
        }
    }

    void addFile(String relativePath) {
        logger.info("Adding file to fileListCache: {}", relativePath);
        Set<String> fileSet = listFiles(upOneDir(relativePath));
        fileSet.add(relativePath);
        putFiles(relativePath, fileSet);
    }

    void removeFile(String relativePath) {
        logger.info("Removing file to fileListCache: {}", relativePath);
        Set<String> fileSet = listFiles(upOneDir(relativePath));
        fileSet.remove(relativePath);
        putFiles(relativePath, fileSet);
    }

    private String upOneDir(String path) {
        String[] dirs = path.split(File.separator);
        return Arrays.stream(dirs)
                .limit(dirs.length - 1)
                .collect(Collectors.joining(File.separator));
    }

    void addEvent(MediaFileEvent mediaFileEvent){
        try{
            jedis.set(MEDIA_EVENTS.name() + mediaFileEvent.getTimestamp(), OBJECT_MAPPER.writeValueAsString(mediaFileEvent));
        } catch (IOException e){
            logger.error("Failure marshalling file paths for cache.", e);
        }
    }
}

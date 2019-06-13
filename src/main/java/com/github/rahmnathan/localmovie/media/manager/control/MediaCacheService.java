package com.github.rahmnathan.localmovie.media.manager.control;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.github.rahmnathan.localmovie.domain.MediaFile;
import com.github.rahmnathan.localmovie.domain.MediaFileEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.rahmnathan.localmovie.domain.CachePrefix.FILE_LIST;
import static com.github.rahmnathan.localmovie.domain.CachePrefix.MEDIA_EVENTS;
import static com.github.rahmnathan.localmovie.domain.CachePrefix.MEDIA_FILE;

@Service
public class MediaCacheService {
    private final Logger logger = LoggerFactory.getLogger(MediaCacheService.class);
    private final ObjectMapper objectMapper;
    private final JedisPool jedisPool;

    public MediaCacheService(@Value("${jedis.host}") String jedisHost) {
        this.jedisPool = new JedisPool(new JedisPoolConfig(), jedisHost);
        this.objectMapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new JavaTimeModule());
    }

    public void addMedia(MediaFile mediaFile) {
        try (Jedis jedis = jedisPool.getResource()) {
            logger.info("Adding media file to cache. Key {} Value {}", mediaFile.getPath(), mediaFile);
            jedis.set(MEDIA_FILE + mediaFile.getPath(), objectMapper.writeValueAsString(mediaFile));
        } catch (IOException e) {
            logger.error("Failure storing mediaFile in cache.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> listFiles(String path) {
        try (Jedis jedis = jedisPool.getResource()) {
            String cacheResponse = jedis.get(FILE_LIST + path);
            if(cacheResponse != null)
                return objectMapper.readValue(cacheResponse, Set.class);
        } catch (IOException e) {
            logger.error("Failure unmarshalling file list from cache.", e);
        }
        return new HashSet<>();
    }

    void putFiles(String path, Set<String> filePaths) {
        try (Jedis jedis = jedisPool.getResource()) {
            logger.debug("Adding files to cache. Key {} Value {}", path, filePaths);
            jedis.set(FILE_LIST + path, objectMapper.writeValueAsString(filePaths));
        } catch (IOException e) {
            logger.error("Failure marshalling file paths for cache.", e);
        }
    }

    void addFile(String relativePath) {
        logger.info("Adding file to fileListCache: {}", relativePath);
        String parentDir = upOneDir(relativePath);
        Set<String> fileSet = listFiles(parentDir);
        fileSet.add(relativePath);
        putFiles(parentDir, fileSet);
    }

    void removeFile(String relativePath) {
        logger.info("Removing file to fileListCache: {}", relativePath);
        String parentDir = upOneDir(relativePath);
        Set<String> fileSet = listFiles(parentDir);
        fileSet.remove(relativePath);
        putFiles(parentDir, fileSet);
    }

    private String upOneDir(String path) {
        String[] dirs = path.split(File.separator);
        return Arrays.stream(dirs)
                .limit(dirs.length - 1)
                .collect(Collectors.joining(File.separator));
    }

    public void addEvent(MediaFileEvent mediaFileEvent) {
        try (Jedis jedis = jedisPool.getResource()) {
            logger.info("Adding MediaFileEvent to cache: {}", mediaFileEvent);
            List<MediaFileEvent> existingEvents = getMediaFileEvents();
            existingEvents.add(mediaFileEvent);
            jedis.set(MEDIA_EVENTS.name(), objectMapper.writeValueAsString(existingEvents));
        } catch (IOException e) {
            logger.error("Failure marshalling file paths for cache.", e);
        }
    }

    void addEvents(List<MediaFileEvent> mediaFileEvents){
        try (Jedis jedis = jedisPool.getResource()) {
            logger.info("Adding MediaFileEvents to cache. Count: {}", mediaFileEvents.size());
            List<MediaFileEvent> existingEvents = getMediaFileEvents();
            existingEvents.addAll(mediaFileEvents);
            jedis.set(MEDIA_EVENTS.name(), objectMapper.writeValueAsString(existingEvents));
        } catch (IOException e) {
            logger.error("Failure marshalling file paths for cache.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<MediaFileEvent> getMediaFileEvents() {
        try (Jedis jedis = jedisPool.getResource()) {
            String cacheResponse = jedis.get(MEDIA_EVENTS.name());
            if (cacheResponse != null) {
                return objectMapper.readValue(cacheResponse, List.class);
            }
        } catch (IOException e) {
            logger.error("Failure getting events from cache.", e);
        }
        return new ArrayList<>();
    }
}
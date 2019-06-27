package com.github.rahmnathan.localmovie.media.manager.control;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.rahmnathan.localmovie.media.manager.control.event.MediaEventType;
import com.github.rahmnathan.localmovie.media.manager.persistence.entity.MediaFile;
import com.github.rahmnathan.localmovie.media.manager.persistence.entity.MediaFileEvent;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.github.rahmnathan.localmovie.domain.CachePrefix.*;
import static org.mockito.Mockito.*;

public class MediaCacheServiceTest {
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private final MediaCacheService mediaCacheService;
    private final Jedis jedis = mock(Jedis.class);
    private static final String PATH = "Movies";

    public MediaCacheServiceTest(){
        JedisPool jedisPool = mock(JedisPool.class);
        when(jedisPool.getResource()).thenReturn(jedis);

        this.mediaCacheService = new MediaCacheService(jedisPool);
    }

    @Test
    public void addMediaTest() throws Exception {
        MediaFile mediaFile = MediaFile.Builder.forPath(PATH).build();
        mediaCacheService.addMedia(mediaFile);

        verify(jedis).set(MEDIA_FILE + PATH, MAPPER.writeValueAsString(mediaFile));
    }

    @Test
    public void deleteMediaTest(){
        mediaCacheService.removeMedia(PATH);

        verify(jedis).del(MEDIA_FILE + PATH);
    }

    @Test
    public void putFilesTest() throws Exception {
        mediaCacheService.putFiles(PATH, Set.of(PATH));

        verify(jedis).set(FILE_LIST + PATH, MAPPER.writeValueAsString(Set.of(PATH)));
    }

    @Test
    public void addFileTest() throws Exception {
        when(jedis.get(FILE_LIST + PATH)).thenReturn(MAPPER.writeValueAsString(Collections.emptySet()));
        mediaCacheService.addFile(PATH + "/Season 2");

        verify(jedis).get(FILE_LIST + PATH);
        verify(jedis).set(FILE_LIST + PATH, MAPPER.writeValueAsString(Set.of(PATH + "/Season 2")));
    }

    @Test
    public void removeFileTest() throws Exception {
        when(jedis.get(FILE_LIST + PATH)).thenReturn(MAPPER.writeValueAsString(Set.of(PATH + "/Season 1")));

        mediaCacheService.removeFile(PATH + "/Season 1");

        verify(jedis).get(FILE_LIST + PATH);
        verify(jedis).set(FILE_LIST + PATH, MAPPER.writeValueAsString(Collections.emptySet()));
    }

    @Test
    public void addEventTest() throws Exception {
        when(jedis.get(MEDIA_EVENTS.name())).thenReturn(MAPPER.writeValueAsString(Collections.emptySet()));

        String mediaPath = PATH + "/testmovie.mkv";
        String mediaEventType = MediaEventType.ENTRY_CREATE.getMovieEventString();
        MediaFileEvent mediaFileEvent = new MediaFileEvent(mediaEventType, null, mediaPath);

        mediaCacheService.addEvent(mediaFileEvent);

        verify(jedis).set(MEDIA_EVENTS.name(), MAPPER.writeValueAsString(Set.of(mediaFileEvent)));
    }

    @Test
    public void addEventsTest() throws Exception {
        String mediaPath = PATH + "/testmovie.mkv";
        String mediaEventType = MediaEventType.ENTRY_CREATE.getMovieEventString();
        MediaFileEvent mediaFileEvent = new MediaFileEvent(mediaEventType, null, mediaPath);

        mediaCacheService.addEvents(List.of(mediaFileEvent));

        verify(jedis).set(MEDIA_EVENTS.name(), MAPPER.writeValueAsString(Set.of(mediaFileEvent)));
    }
}

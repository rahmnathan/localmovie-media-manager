package com.github.rahmnathan.localmovie.media.manager.control.event;

import com.github.rahmnathan.localmovie.media.manager.control.MediaCacheService;
import com.github.rahmnathan.localmovie.media.manager.persistence.repository.MediaFileEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class MediaEventServiceTest {

    @MockBean
    private MediaCacheService cacheService;

    @MockBean
    private MediaFileEventRepository eventRepository;

    private final MediaEventService eventService;

    @Autowired
    public MediaEventServiceTest(MediaEventService eventService){
        this.eventService = eventService;
    }

    @Test
    public void initializeCacheTest(){
        eventService.initializeCache();
    }
}

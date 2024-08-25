package com.github.rahmnathan.localmovie.persistence.control;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class MediaRepositoryMonitorTest {
    private final MediaRepositoryMonitor repositoryMonitor;

    @Autowired
    MediaRepositoryMonitorTest(MediaRepositoryMonitor repositoryMonitor) {
        this.repositoryMonitor = repositoryMonitor;
    }

    @Test
    void checkForEmptyValuesTest() {
        repositoryMonitor.checkForEmptyValues();
    }
}

package com.github.rahmnathan.localmovie.persistence.control;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MediaRepositoryMonitorTest {
    private final MediaRepositoryMonitor repositoryMonitor;

    @Autowired
    public MediaRepositoryMonitorTest(MediaRepositoryMonitor repositoryMonitor) {
        this.repositoryMonitor = repositoryMonitor;
    }

    @Test
    public void checkForEmptyValuesTest() {
        repositoryMonitor.checkForEmptyValues();
    }
}

package com.github.rahmnathan.localmovie.media.manager.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class PushTokenRepositoryMonitor {
    private final Logger logger = LoggerFactory.getLogger(PushTokenRepositoryMonitor.class.getName());
    private final AndroidPushTokenRepository pushTokenRepository;

    public PushTokenRepositoryMonitor(AndroidPushTokenRepository pushTokenRepository) {
        this.pushTokenRepository = pushTokenRepository;
    }

    @Scheduled(fixedDelay = 86400000)
    public void checkForStaleAndroidClients(){
        LocalDate purgeDate = LocalDate.now().minusDays(21);
        logger.info("Purging PushClients last accessed before Date: {}", purgeDate.format(DateTimeFormatter.ISO_DATE));
        pushTokenRepository.deleteAllByLastAccessBefore(purgeDate);
    }
}

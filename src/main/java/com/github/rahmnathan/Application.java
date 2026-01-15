package com.github.rahmnathan;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;
import java.nio.charset.Charset;

@Slf4j
@EnableAspectJAutoProxy
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    public void printEnv() {
        log.info("file.encoding: {}", Charset.defaultCharset().displayName());
        log.info("sun.jnu.encoding: {}", System.getProperty("sun.jnu.encoding"));
    }
}


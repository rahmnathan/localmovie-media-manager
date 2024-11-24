package com.github.rahmnathan.localmovie.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.inmemory.InMemoryLockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
public class DistributedLocking {

    @Bean
    public LockRepository defaultLockRepository(DataSource dataSource){
        DefaultLockRepository repository = new DefaultLockRepository(dataSource);
        repository.setTimeToLive(Duration.of(1, ChronoUnit.MINUTES).toMillisPart());
        return repository;
    }

    @Bean
    public JdbcLockRegistry jdbcLockRegistry(LockRepository lockRepository){
        return new JdbcLockRegistry(lockRepository);
    }

    @Bean
    @Profile("test")
    public LockProvider lockProvider() {
        return new InMemoryLockProvider();
    }

    @Bean
    @Profile("!test")
    public LockProvider jdbcLockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build()
        );
    }
}

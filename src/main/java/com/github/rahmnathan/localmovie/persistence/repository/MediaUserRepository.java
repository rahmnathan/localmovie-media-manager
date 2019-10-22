package com.github.rahmnathan.localmovie.persistence.repository;

import com.github.rahmnathan.localmovie.persistence.entity.MediaUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediaUserRepository extends JpaRepository<MediaUser, Long> {
    Optional<MediaUser> findByUserId(String userId);
}

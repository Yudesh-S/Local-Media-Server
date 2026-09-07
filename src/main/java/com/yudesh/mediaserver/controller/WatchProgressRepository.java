package com.yudesh.mediaserver.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchProgressRepository
        extends JpaRepository<WatchProgress, Long> {

    Optional<WatchProgress> findByUser_IdAndMediaTypeAndMediaId(
            Long userId,
            WatchProgress.MediaType mediaType,
            String mediaId
    );

    List<WatchProgress> findAllByUser_IdOrderByUpdatedAtDesc(
            Long userId
    );

    void deleteAllByUser_Id(Long userId);
}
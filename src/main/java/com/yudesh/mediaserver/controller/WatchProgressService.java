package com.yudesh.mediaserver.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WatchProgressService {

    private final WatchProgressRepository progressRepository;
    private final UserProfileRepository userRepository;

    public WatchProgressService(
            WatchProgressRepository progressRepository,
            UserProfileRepository userRepository
    ) {
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
    }

    public List<WatchProgress> getUserProgress(Long userId) {
        requireUser(userId);

        return progressRepository
                .findAllByUser_IdOrderByUpdatedAtDesc(userId);
    }

    public Optional<WatchProgress> getProgress(
            Long userId,
            WatchProgress.MediaType mediaType,
            String mediaId
    ) {
        requireUser(userId);

        return progressRepository
                .findByUser_IdAndMediaTypeAndMediaId(
                        userId,
                        mediaType,
                        cleanMediaId(mediaId)
                );
    }
    
    @Transactional
    public WatchProgress saveProgress(
            Long userId,
            WatchProgress.MediaType mediaType,
            String mediaId,
            double positionSeconds,
            double durationSeconds
    ) {
        UserProfile user = requireUser(userId);

        if (mediaType == null) {
            throw new IllegalArgumentException(
                    "Media type is required."
            );
        }

        String cleanedMediaId = cleanMediaId(mediaId);
        validateTimes(positionSeconds, durationSeconds);

        Optional<WatchProgress> existingProgress = progressRepository
                .findByUser_IdAndMediaTypeAndMediaId(
                        userId,
                        mediaType,
                        cleanedMediaId
                );

        WatchProgress progress;

        if (existingProgress.isPresent()) {
            progress = existingProgress.get();
            progress.updateProgress(
                    positionSeconds,
                    durationSeconds
            );
        } else {
            progress = new WatchProgress(
                    user,
                    mediaType,
                    cleanedMediaId,
                    positionSeconds,
                    durationSeconds
            );
        }

        return progressRepository.save(progress);
    }

    @Transactional
    public void deleteAllForUser(Long userId) {
        progressRepository.deleteAllByUser_Id(userId);
    }

    private UserProfile requireUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID is required."
            );
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User does not exist."
                ));
    }

    private String cleanMediaId(String mediaId) {
        if (mediaId == null || mediaId.isBlank()) {
            throw new IllegalArgumentException(
                    "Media ID is required."
            );
        }

        String cleanedMediaId = mediaId.trim();

        if (cleanedMediaId.length() > 36) {
            throw new IllegalArgumentException(
                    "Media ID is too long."
            );
        }

        return cleanedMediaId;
    }

    private void validateTimes(
            double positionSeconds,
            double durationSeconds
    ) {
        if (!Double.isFinite(positionSeconds)
                || !Double.isFinite(durationSeconds)) {
            throw new IllegalArgumentException(
                    "Playback times must be valid numbers."
            );
        }

        if (positionSeconds < 0) {
            throw new IllegalArgumentException(
                    "Playback position cannot be negative."
            );
        }

        if (durationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Video duration must be greater than zero."
            );
        }
    }
}
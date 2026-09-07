package com.yudesh.mediaserver.controller;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progress")
public class WatchProgressController {

    private final WatchProgressService progressService;

    public WatchProgressController(
            WatchProgressService progressService
    ) {
        this.progressService = progressService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserProgress(
            @PathVariable Long userId
    ) {
        try {
            List<ProgressResponse> progress = progressService
                    .getUserProgress(userId)
                    .stream()
                    .map(item -> toResponse(userId, item))
                    .toList();

            return ResponseEntity.ok(progress);
        } catch (IllegalArgumentException exception) {
            return badRequest(exception);
        }
    }

    @GetMapping("/{userId}/{mediaType}/{mediaId}")
    public ResponseEntity<?> getProgress(
            @PathVariable Long userId,
            @PathVariable String mediaType,
            @PathVariable String mediaId
    ) {
        try {
            WatchProgress.MediaType parsedType =
                    parseMediaType(mediaType);

            Optional<WatchProgress> progress =
                    progressService.getProgress(
                            userId,
                            parsedType,
                            mediaId
                    );

            if (progress.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(
                    toResponse(userId, progress.get())
            );
        } catch (IllegalArgumentException exception) {
            return badRequest(exception);
        }
    }

    @PutMapping("/{userId}/{mediaType}/{mediaId}")
    public ResponseEntity<?> saveProgress(
            @PathVariable Long userId,
            @PathVariable String mediaType,
            @PathVariable String mediaId,
            @RequestBody SaveProgressRequest request
    ) {
        try {
            WatchProgress.MediaType parsedType =
                    parseMediaType(mediaType);

            WatchProgress progress =
                    progressService.saveProgress(
                            userId,
                            parsedType,
                            mediaId,
                            request.positionSeconds(),
                            request.durationSeconds()
                    );

            return ResponseEntity.ok(
                    toResponse(userId, progress)
            );
        } catch (IllegalArgumentException exception) {
            return badRequest(exception);
        }
    }

    private WatchProgress.MediaType parseMediaType(
            String mediaType
    ) {
        try {
            return WatchProgress.MediaType.valueOf(
                    mediaType.trim()
                            .toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Media type must be movie or episode."
            );
        }
    }

    private ProgressResponse toResponse(
            Long userId,
            WatchProgress progress
    ) {
        return new ProgressResponse(
                userId,
                progress.getMediaType()
                        .name()
                        .toLowerCase(Locale.ROOT),
                progress.getMediaId(),
                progress.getPositionSeconds(),
                progress.getDurationSeconds(),
                progress.isCompleted(),
                progress.getUpdatedAt()
        );
    }

    private ResponseEntity<ErrorResponse> badRequest(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse(exception.getMessage())
        );
    }

    public record SaveProgressRequest(
            double positionSeconds,
            double durationSeconds
    ) {
    }

    public record ProgressResponse(
            Long userId,
            String mediaType,
            String mediaId,
            double positionSeconds,
            double durationSeconds,
            boolean completed,
            Instant updatedAt
    ) {
    }

    public record ErrorResponse(String error) {
    }
}
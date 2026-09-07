package com.yudesh.mediaserver.controller;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "watch_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_user_media_progress",
                columnNames = {"user_id", "media_type", "media_id"}
        )
)
public class WatchProgress {

    public enum MediaType {
        MOVIE,
        EPISODE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private MediaType mediaType;

    @Column(name = "media_id", nullable = false, length = 36)
    private String mediaId;

    @Column(name = "position_seconds", nullable = false)
    private double positionSeconds;

    @Column(name = "duration_seconds", nullable = false)
    private double durationSeconds;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WatchProgress() {
        // Required by JPA.
    }

    public WatchProgress(
            UserProfile user,
            MediaType mediaType,
            String mediaId,
            double positionSeconds,
            double durationSeconds
    ) {
        this.user = user;
        this.mediaType = mediaType;
        this.mediaId = mediaId;
        updateProgress(positionSeconds, durationSeconds);
    }

    public void updateProgress(
            double positionSeconds,
            double durationSeconds
    ) {
        double cleanDuration = Math.max(0, durationSeconds);
        double cleanPosition = Math.max(0, positionSeconds);

        if (cleanDuration > 0) {
            cleanPosition = Math.min(cleanPosition, cleanDuration);
        }

        this.positionSeconds = cleanPosition;
        this.durationSeconds = cleanDuration;
        this.completed = cleanDuration > 0
                && cleanPosition >= cleanDuration * 0.95;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return user.getId();
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public String getMediaId() {
        return mediaId;
    }

    public double getPositionSeconds() {
        return positionSeconds;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
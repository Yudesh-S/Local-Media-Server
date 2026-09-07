package com.yudesh.mediaserver.controller;

public class Episode {

    private final String id;
    private final String title;
    private final int seasonNumber;
    private final int episodeNumber;
    private final String fileName;

    public Episode(
            String id,
            String title,
            int seasonNumber,
            int episodeNumber,
            String fileName
    ) {
        this.id = id;
        this.title = title;
        this.seasonNumber = seasonNumber;
        this.episodeNumber = episodeNumber;
        this.fileName = fileName;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getSeasonNumber() {
        return seasonNumber;
    }

    public int getEpisodeNumber() {
        return episodeNumber;
    }

    public String getFileName() {
        return fileName;
    }
}

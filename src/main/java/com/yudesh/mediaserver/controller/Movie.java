package com.yudesh.mediaserver.controller;

public class Movie {

    private final String id;
    private final String title;
    private final String fileName;

    public Movie(String id, String title, String fileName) {
        this.id = id;
        this.title = title;
        this.fileName = fileName;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getFileName() {
        return fileName;
    }
}

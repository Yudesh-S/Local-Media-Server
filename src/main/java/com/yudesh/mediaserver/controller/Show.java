package com.yudesh.mediaserver.controller;

public class Show {

    private final String id;
    private final String title;

    public Show(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}

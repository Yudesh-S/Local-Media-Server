============================================================
                    local-media-server
============================================================

A Java Spring Boot media server for streaming movies and TV shows through a browser on a local network.

Features
--------
[ Movies ] [ TV Shows ] [ Search ] [ Playback ]
[ Subtitles ] [ Profiles ] [ Watch Progress ] [ Console ]

Requirements

Java 21+
Movie and TV-show .mp4 files

Setup

Create this file:

src/main/resources/application.properties

Add your media paths:

spring.application.name=media-server
server.address=0.0.0.0
server.port=8080

media.movies.path=C:/media-library/movies
media.shows.path=C:/media-library/shows

Organize media like this:

C:\media-library\
├── movies\
│   └── Shrek.mp4
└── shows\
    └── Example Show\
        └── S01E01 - Pilot.mp4

Optional subtitles use the same filename with .vtt:

S01E01 - Pilot.vtt

Run

From the project folder:

.\mvnw.cmd spring-boot:run

Open locally:

http://localhost:8080

From another device on the same network:

http://SERVER_IP:8080

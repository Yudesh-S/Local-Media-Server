package com.yudesh.mediaserver.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/shows")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @GetMapping
    public List<Show> getShows() throws IOException {
        return showService.getShows();
    }

    @GetMapping("/{showId}/episodes")
    public List<Episode> getEpisodes(
            @PathVariable String showId
    ) throws IOException {
        return showService.getEpisodes(showId);
    }

    @GetMapping("/episodes/{episodeId}/stream")
    public ResponseEntity<Resource> streamEpisode(
            @PathVariable String episodeId
    ) throws IOException {

        Path episodePath = showService.findEpisodePath(episodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Episode was not found"
                ));

        Resource episodeResource =
                new FileSystemResource(episodePath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .contentLength(Files.size(episodePath))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(episodeResource);
    }

    @GetMapping("/episodes/{episodeId}/subtitles")
    public ResponseEntity<Resource> getEpisodeSubtitles(
            @PathVariable String episodeId
    ) throws IOException {

        Path subtitlePath =
                showService.findEpisodeSubtitlePath(episodeId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Episode or subtitle file was not found"
                        ));

        Resource subtitleResource =
                new FileSystemResource(subtitlePath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "text/vtt;charset=UTF-8"
                ))
                .contentLength(Files.size(subtitlePath))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(subtitleResource);
    }
}
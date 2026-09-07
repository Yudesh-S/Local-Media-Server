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
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public List<Movie> getMovies() throws IOException {
        return movieService.getMovies();
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> streamMovie(
            @PathVariable String id
    ) throws IOException {

        Path moviePath = movieService.findMoviePath(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Movie was not found"
                ));

        Resource movieResource = new FileSystemResource(moviePath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .contentLength(Files.size(moviePath))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(movieResource);
    }

    @GetMapping("/{id}/subtitles")
    public ResponseEntity<Resource> getMovieSubtitles(
            @PathVariable String id
    ) throws IOException {

        Path subtitlePath = movieService.findMovieSubtitlePath(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Movie or subtitle file was not found"
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
package com.yudesh.mediaserver.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MovieService {

    private final Path moviesDirectory;

    public MovieService(
            @Value("${media.movies.path}") String moviesPath
    ) {
        this.moviesDirectory = Path.of(moviesPath.trim())
                .toAbsolutePath()
                .normalize();
    }

    public List<Movie> getMovies() throws IOException {
        try (Stream<Path> files = Files.list(moviesDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(this::isMp4File)
                    .sorted(Comparator.comparing(
                            path -> path.getFileName()
                                    .toString()
                                    .toLowerCase(Locale.ROOT)
                    ))
                    .map(this::createMovie)
                    .toList();
        }
    }

    public Optional<Path> findMoviePath(String id) throws IOException {
        try (Stream<Path> files = Files.list(moviesDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(this::isMp4File)
                    .filter(path -> createId(
                            path.getFileName().toString()
                    ).equals(id))
                    .findFirst();
        }
    }

    public Optional<Path> findMovieSubtitlePath(String id)
            throws IOException {

        Optional<Path> moviePath = findMoviePath(id);

        if (moviePath.isEmpty()) {
            return Optional.empty();
        }

        Path videoPath = moviePath.get();

        String fileName = videoPath.getFileName().toString();

        String baseName = fileName.substring(
                0,
                fileName.lastIndexOf('.')
        );

        Path subtitlePath = videoPath.resolveSibling(
                baseName + ".vtt"
        );

        if (Files.isRegularFile(subtitlePath)
                && Files.isReadable(subtitlePath)) {
            return Optional.of(subtitlePath);
        }

        return Optional.empty();
    }

    private boolean isMp4File(Path path) {
        String fileName = path.getFileName().toString();

        return fileName.toLowerCase(Locale.ROOT).endsWith(".mp4");
    }

    private Movie createMovie(Path path) {
        String fileName = path.getFileName().toString();

        String title = fileName.substring(
                0,
                fileName.lastIndexOf('.')
        );

        String id = createId(fileName);

        return new Movie(id, title, fileName);
    }

    private String createId(String fileName) {
        return UUID.nameUUIDFromBytes(
                fileName.toLowerCase(Locale.ROOT)
                        .getBytes(StandardCharsets.UTF_8)
        ).toString();
    }
}
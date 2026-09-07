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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ShowService {

    private static final Pattern EPISODE_PATTERN = Pattern.compile(
            "^S(\\d{1,2})E(\\d{1,3})\\s*-\\s*(.+)\\.mp4$",
            Pattern.CASE_INSENSITIVE
    );

    private final Path showsDirectory;

    public ShowService(@Value("${media.shows.path}") String showsPath) {
        this.showsDirectory = Path.of(showsPath.trim())
                .toAbsolutePath()
                .normalize();
    }

    public List<Show> getShows() throws IOException {
        try (Stream<Path> paths = Files.list(showsDirectory)) {
            return paths
                    .filter(Files::isDirectory)
                    .map(this::createShow)
                    .sorted(Comparator.comparing(
                            Show::getTitle,
                            String.CASE_INSENSITIVE_ORDER
                    ))
                    .toList();
        }
    }

    public List<Episode> getEpisodes(String showId) throws IOException {
        Optional<Path> showPath = findShowPath(showId);

        if (showPath.isEmpty()) {
            return List.of();
        }

        try (Stream<Path> paths = Files.walk(showPath.get())) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(this::isEpisodeFile)
                    .map(this::createEpisode)
                    .sorted(
                            Comparator
                                    .comparingInt(Episode::getSeasonNumber)
                                    .thenComparingInt(Episode::getEpisodeNumber)
                    )
                    .toList();
        }
    }

    public Optional<Path> findEpisodePath(String episodeId) throws IOException {
        try (Stream<Path> paths = Files.walk(showsDirectory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(this::isEpisodeFile)
                    .filter(path -> createEpisodeId(path).equals(episodeId))
                    .findFirst();
        }
    }

    private Optional<Path> findShowPath(String showId) throws IOException {
        try (Stream<Path> paths = Files.list(showsDirectory)) {
            return paths
                    .filter(Files::isDirectory)
                    .filter(path -> createShowId(path).equals(showId))
                    .findFirst();
        }
    }

    private Show createShow(Path showPath) {
        String title = showPath.getFileName().toString();
        String id = createShowId(showPath);

        return new Show(id, title);
    }

    private Episode createEpisode(Path episodePath) {
        String fileName = episodePath.getFileName().toString();
        Matcher matcher = EPISODE_PATTERN.matcher(fileName);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid episode filename: " + fileName
            );
        }

        int seasonNumber = Integer.parseInt(matcher.group(1));
        int episodeNumber = Integer.parseInt(matcher.group(2));
        String title = matcher.group(3).trim();
        String id = createEpisodeId(episodePath);

        return new Episode(
                id,
                title,
                seasonNumber,
                episodeNumber,
                fileName
        );
    }

    private boolean isEpisodeFile(Path path) {
        String fileName = path.getFileName().toString();
        return EPISODE_PATTERN.matcher(fileName).matches();
    }

    private String createShowId(Path showPath) {
        String showName = showPath.getFileName().toString();
        return createStableId("show:" + showName);
    }

    private String createEpisodeId(Path episodePath) {
        String relativePath = showsDirectory
                .relativize(episodePath.toAbsolutePath().normalize())
                .toString()
                .replace('\\', '/');

        return createStableId("episode:" + relativePath);
    }

    private String createStableId(String value) {
        return UUID.nameUUIDFromBytes(
                value.toLowerCase(Locale.ROOT)
                        .getBytes(StandardCharsets.UTF_8)
        ).toString();
    }
    public Optional<Path> findEpisodeSubtitlePath(String episodeId)
            throws IOException {

        Optional<Path> episodePath = findEpisodePath(episodeId);

        if (episodePath.isEmpty()) {
            return Optional.empty();
        }

        Path videoPath = episodePath.get();

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
}
const player = document.getElementById("player");
const currentTitle = document.getElementById("current-title");

const moviesTab = document.getElementById("movies-tab");
const showsTab = document.getElementById("shows-tab");
const moviesSection = document.getElementById("movies-section");
const showsSection = document.getElementById("shows-section");

const movieList = document.getElementById("movie-list");
const movieStatus = document.getElementById("movie-status");
const showList = document.getElementById("show-list");
const showStatus = document.getElementById("show-status");
const librarySearch = document.getElementById("library-search");

const userSelect = document.getElementById("user-select");
const deleteUserButton =
    document.getElementById("delete-user-button");
const createUserForm =
    document.getElementById("create-user-form");
const newUserName =
    document.getElementById("new-user-name");
const createUserButton =
    document.getElementById("create-user-button");
const userStatus = document.getElementById("user-status");

const consoleOutput =
    document.getElementById("console-output");
const clearConsoleButton =
    document.getElementById("clear-console-button");

const SELECTED_USER_STORAGE_KEY =
    "mediaServerSelectedUserId";

const PROGRESS_SAVE_INTERVAL = 5000;

let selectedUserId = null;
let currentMediaType = null;
let currentMediaId = null;
let hasPlaybackStarted = false;
let lastSavedPosition = null;
let lastProgressSaveTime = 0;

let movies = [];
let shows = [];

const episodeCache = new Map();


moviesTab.addEventListener("click", () => {
    showLibrary("movies");
});

showsTab.addEventListener("click", () => {
    showLibrary("shows");
});

librarySearch.addEventListener("input", () => {
    renderMovies();
    renderShows();
});

clearConsoleButton.addEventListener("click", () => {
    consoleOutput.innerHTML = "";
    consoleOutput.appendChild(userStatus);
    appendConsoleMessage("Console cleared.", "info");
});


userSelect.addEventListener("change", async () => {
    await saveCurrentProgress();

    selectUser(userSelect.value);

    if (selectedUserId === null) {
        setUserStatus(
            "No profile selected. Progress will not be saved."
        );
    } else {
        setUserStatus(
            "Tracking progress for " +
            getSelectedUserName() +
            ".",
            "success"
        );
    }

    if (currentMediaId !== null) {
        restoreCurrentProgress(false);
    }
});


createUserForm.addEventListener("submit", async event => {
    event.preventDefault();

    const name = newUserName.value.trim();

    if (name.length === 0) {
        setUserStatus(
            "Enter a name for the new profile.",
            "error"
        );
        newUserName.focus();
        return;
    }

    await saveCurrentProgress();

    createUserButton.disabled = true;

    try {
        const response = await fetch("/api/users", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ name })
        });

        if (!response.ok) {
            throw new Error("Create-user request failed.");
        }

        const createdUser = await response.json();

        newUserName.value = "";

        await loadUsers(String(createdUser.id));

        setUserStatus(
            "Created and selected " +
            createdUser.name +
            ".",
            "success"
        );

        if (currentMediaId !== null) {
            restoreCurrentProgress(false);
        }
    } catch (error) {
        setUserStatus(
            "Could not create that user. " +
            "The name may already exist.",
            "error"
        );

        console.error(error);
    } finally {
        createUserButton.disabled = false;
    }
});


deleteUserButton.addEventListener("click", async () => {
    if (selectedUserId === null) {
        return;
    }

    const userName = getSelectedUserName();

    const confirmed = window.confirm(
        "Delete " +
        userName +
        " and all of this profile's watch progress?"
    );

    if (!confirmed) {
        return;
    }

    deleteUserButton.disabled = true;

    try {
        const response = await fetch(
            "/api/users/" +
            encodeURIComponent(selectedUserId),
            {
                method: "DELETE"
            }
        );

        if (!response.ok) {
            throw new Error("Delete-user request failed.");
        }

        selectUser(null);
        await loadUsers(null);

        setUserStatus(
            "Deleted " +
            userName +
            ". Select or create a profile."
        );
    } catch (error) {
        deleteUserButton.disabled = false;

        setUserStatus(
            "Could not delete that user.",
            "error"
        );

        console.error(error);
    }
});


player.addEventListener("play", () => {
    hasPlaybackStarted = true;
});


player.addEventListener("timeupdate", () => {
    const now = Date.now();

    if (
        now - lastProgressSaveTime >=
        PROGRESS_SAVE_INTERVAL
    ) {
        lastProgressSaveTime = now;
        saveCurrentProgress();
    }
});


player.addEventListener("pause", () => {
    saveCurrentProgress();
});


player.addEventListener("ended", () => {
    saveCurrentProgress(false, true);
});


document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "hidden") {
        saveCurrentProgress();
    }
});


window.addEventListener("pagehide", () => {
    saveCurrentProgress(true, true);
});


function showLibrary(libraryName) {
    const showingMovies = libraryName === "movies";

    moviesSection.classList.toggle(
        "hidden",
        !showingMovies
    );

    showsSection.classList.toggle(
        "hidden",
        showingMovies
    );

    moviesTab.classList.toggle(
        "active",
        showingMovies
    );

    showsTab.classList.toggle(
        "active",
        !showingMovies
    );
}


async function loadUsers(
    preferredUserId = getRememberedUserId()
) {
    try {
        const response = await fetch("/api/users");

        if (!response.ok) {
            throw new Error("User request failed.");
        }

        const users = await response.json();

        userSelect.innerHTML = "";

        const emptyOption =
            document.createElement("option");

        emptyOption.value = "";
        emptyOption.textContent = "Select a user";

        userSelect.appendChild(emptyOption);

        users.forEach(user => {
            const option =
                document.createElement("option");

            option.value = String(user.id);
            option.textContent = user.name;

            userSelect.appendChild(option);
        });

        const preferredUserExists = users.some(
            user =>
                String(user.id) ===
                String(preferredUserId)
        );

        if (
            preferredUserId !== null &&
            preferredUserExists
        ) {
            selectUser(String(preferredUserId));

            setUserStatus(
                "Tracking progress for " +
                getSelectedUserName() +
                ".",
                "success"
            );
        } else {
            selectUser(null);

            if (users.length === 0) {
                setUserStatus(
                    "Create your first profile " +
                    "to begin tracking progress."
                );
            } else {
                setUserStatus(
                    "Select a profile to track " +
                    "watch progress."
                );
            }
        }
    } catch (error) {
        selectUser(null);

        userSelect.disabled = true;

        setUserStatus(
            "Could not load user profiles.",
            "error"
        );

        console.error(error);
    }
}


function selectUser(userId) {
    selectedUserId =
        userId ? String(userId) : null;

    userSelect.value =
        selectedUserId ?? "";

    userSelect.disabled = false;

    deleteUserButton.disabled =
        selectedUserId === null;

    lastSavedPosition = null;
    lastProgressSaveTime = 0;

    try {
        if (selectedUserId === null) {
            localStorage.removeItem(
                SELECTED_USER_STORAGE_KEY
            );
        } else {
            localStorage.setItem(
                SELECTED_USER_STORAGE_KEY,
                selectedUserId
            );
        }
    } catch (error) {
        console.warn(
            "Could not remember the selected user.",
            error
        );
    }
}


function getRememberedUserId() {
    try {
        return localStorage.getItem(
            SELECTED_USER_STORAGE_KEY
        );
    } catch (error) {
        console.warn(
            "Could not read the remembered user.",
            error
        );

        return null;
    }
}


function getSelectedUserName() {
    const option =
        userSelect.options[userSelect.selectedIndex];

    return option
        ? option.textContent
        : "the selected user";
}


function setUserStatus(message, type = "") {
    userStatus.textContent = message;

    userStatus.classList.remove(
        "success",
        "error"
    );

    if (type !== "") {
        userStatus.classList.add(type);
    }

    appendConsoleMessage(
        message,
        type || "info"
    );
}


function appendConsoleMessage(
    message,
    type = "info"
) {
    const entry =
        document.createElement("p");

    const timestamp =
        new Date().toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit"
        });

    entry.className =
        "console-entry " + type;

    entry.dataset.time = timestamp;
    entry.textContent = message;

    consoleOutput.appendChild(entry);

    while (consoleOutput.children.length > 100) {
        consoleOutput.firstElementChild.remove();
    }

    consoleOutput.scrollTop =
        consoleOutput.scrollHeight;
}


async function loadMovies() {
    try {
        const response =
            await fetch("/api/movies");

        if (!response.ok) {
            throw new Error(
                "Movie request failed."
            );
        }

        movies = await response.json();

        appendConsoleMessage(
            "Loaded " +
            movies.length +
            " movie(s).",
            "success"
        );

        if (movies.length === 0) {
            movieStatus.textContent =
                "No movies were found.";

            return;
        }

        renderMovies();
    } catch (error) {
        movieStatus.textContent =
            "Could not load movies.";

        console.error(error);
    }
}


function renderMovies() {
    const query =
        getLibrarySearchQuery();

    const visibleMovies =
        movies.filter(movie =>
            movie.title
                .toLowerCase()
                .includes(query)
        );

    movieList.innerHTML = "";

    movieStatus.textContent =
        visibleMovies.length === 1
            ? "1 movie available"
            : visibleMovies.length +
              " movies available";

    if (visibleMovies.length === 0) {
        movieList.appendChild(
            createEmptyMessage(
                query
                    ? "No movies match your search."
                    : "No movies were found."
            )
        );

        return;
    }

    visibleMovies.forEach(movie => {
        const button =
            createMediaButton(movie.title);

        button.addEventListener(
            "click",
            () => {
                playMovie(movie);
            }
        );

        movieList.appendChild(button);
    });
}


async function loadShows() {
    try {
        const response =
            await fetch("/api/shows");

        if (!response.ok) {
            throw new Error(
                "Show request failed."
            );
        }

        shows = await response.json();

        appendConsoleMessage(
            "Loaded " +
            shows.length +
            " show(s).",
            "success"
        );

        if (shows.length === 0) {
            showStatus.textContent =
                "No shows were found.";

            return;
        }

        renderShows();
    } catch (error) {
        showStatus.textContent =
            "Could not load shows.";

        console.error(error);
    }
}


function renderShows() {
    const query =
        getLibrarySearchQuery();

    const visibleShows =
        shows.filter(show =>
            show.title
                .toLowerCase()
                .includes(query)
        );

    showList.innerHTML = "";

    showStatus.textContent =
        visibleShows.length === 1
            ? "1 show available"
            : visibleShows.length +
              " shows available";

    if (visibleShows.length === 0) {
        showList.appendChild(
            createEmptyMessage(
                query
                    ? "No shows match your search."
                    : "No shows were found."
            )
        );

        return;
    }

    visibleShows.forEach(show => {
        const details =
            document.createElement("details");

        details.className =
            "show-details";

        const summary =
            document.createElement("summary");

        summary.textContent =
            show.title;

        details.appendChild(summary);

        const episodeContainer =
            document.createElement("div");

        episodeContainer.className =
            "show-episode-list";

        episodeContainer.textContent =
            "Open to load episodes...";

        details.appendChild(
            episodeContainer
        );

        details.addEventListener(
            "toggle",
            () => {
                if (details.open) {
                    loadEpisodes(
                        show,
                        episodeContainer
                    );
                }
            }
        );

        showList.appendChild(details);
    });
}


async function loadEpisodes(
    show,
    container
) {
    if (episodeCache.has(show.id)) {
        renderEpisodeTree(
            show,
            episodeCache.get(show.id),
            container
        );

        return;
    }

    container.textContent =
        "Loading episodes...";

    try {
        const response =
            await fetch(
                "/api/shows/" +
                encodeURIComponent(show.id) +
                "/episodes"
            );

        if (!response.ok) {
            throw new Error(
                "Episode request failed."
            );
        }

        const episodes =
            await response.json();

        appendConsoleMessage(
            "Loaded " +
            episodes.length +
            " episode(s) for " +
            show.title +
            ".",
            "success"
        );

        if (episodes.length === 0) {
            container.textContent =
                "No episodes were found.";

            return;
        }

        episodeCache.set(
            show.id,
            episodes
        );

        renderEpisodeTree(
            show,
            episodes,
            container
        );
    } catch (error) {
        container.textContent =
            "Could not load episodes.";

        console.error(error);
    }
}


function renderEpisodeTree(
    show,
    episodes,
    container
) {
    container.innerHTML = "";

    const seasons = new Map();

    episodes.forEach(episode => {
        if (!seasons.has(
            episode.seasonNumber
        )) {
            seasons.set(
                episode.seasonNumber,
                []
            );
        }

        seasons
            .get(episode.seasonNumber)
            .push(episode);
    });

    [...seasons.entries()]
        .sort((a, b) => a[0] - b[0])
        .forEach(
            ([seasonNumber, seasonEpisodes]) => {
                const seasonDetails =
                    document.createElement(
                        "details"
                    );

                seasonDetails.className =
                    "season-details";

                const seasonSummary =
                    document.createElement(
                        "summary"
                    );

                seasonSummary.textContent =
                    "Season " + seasonNumber;

                seasonDetails.appendChild(
                    seasonSummary
                );

                const seasonEpisodeList =
                    document.createElement(
                        "div"
                    );

                seasonEpisodeList.className =
                    "season-episode-list";

                seasonEpisodes
                    .sort(
                        (a, b) =>
                            a.episodeNumber -
                            b.episodeNumber
                    )
                    .forEach(episode => {
                        const button =
                            document.createElement(
                                "button"
                            );

                        button.className =
                            "episode-button";

                        button.textContent =
                            "Episode " +
                            episode.episodeNumber +
                            ": " +
                            episode.title;

                        button.addEventListener(
                            "click",
                            () => {
                                playEpisode(
                                    show,
                                    episode
                                );
                            }
                        );

                        seasonEpisodeList
                            .appendChild(button);
                    });

                seasonDetails.appendChild(
                    seasonEpisodeList
                );

                container.appendChild(
                    seasonDetails
                );
            }
        );
}


function getLibrarySearchQuery() {
    return librarySearch.value
        .trim()
        .toLowerCase();
}


function createEmptyMessage(message) {
    const element =
        document.createElement("p");

    element.className =
        "empty-library-message";

    element.textContent =
        message;

    return element;
}


function createMediaButton(title) {
    const button =
        document.createElement("button");

    button.className =
        "media-button";

    button.textContent =
        title;

    return button;
}


async function playMovie(movie) {
    await saveCurrentProgress();

    appendConsoleMessage(
        "Loading movie: " +
        movie.title,
        "info"
    );

    currentMediaType = "MOVIE";
    currentMediaId = movie.id;

    currentTitle.textContent =
        movie.title;

    prepareNewMedia();

    player.src =
        "/api/movies/" +
        encodeURIComponent(movie.id) +
        "/stream";

    startPlayer();
}


async function playEpisode(
    show,
    episode
) {
    await saveCurrentProgress();

    appendConsoleMessage(
        "Loading episode: " +
        show.title +
        " S" +
        episode.seasonNumber +
        "E" +
        episode.episodeNumber,
        "info"
    );

    currentMediaType = "EPISODE";
    currentMediaId = episode.id;

    currentTitle.textContent =
        show.title +
        " — S" +
        episode.seasonNumber +
        " E" +
        episode.episodeNumber +
        " — " +
        episode.title;

    prepareNewMedia();

    player.src =
        "/api/shows/episodes/" +
        encodeURIComponent(episode.id) +
        "/stream";

    startPlayer();
}


let subtitleLoadVersion = 0;


function prepareNewMedia() {
    hasPlaybackStarted = false;
    lastSavedPosition = null;
    lastProgressSaveTime = 0;

    clearSubtitles();
}


function startPlayer() {
    player.load();

    loadCurrentSubtitles();

    restoreCurrentProgress(true);
}


function clearSubtitles() {
    subtitleLoadVersion++;

    player
        .querySelectorAll("track")
        .forEach(trackElement => {
            trackElement.track.mode =
                "disabled";

            trackElement.remove();
        });
}


async function loadCurrentSubtitles() {
    const version =
        ++subtitleLoadVersion;

    const mediaType =
        currentMediaType;

    const mediaId =
        currentMediaId;

    if (mediaId === null) {
        return;
    }

    let subtitleUrl;

    if (mediaType === "MOVIE") {
        subtitleUrl =
            "/api/movies/" +
            encodeURIComponent(mediaId) +
            "/subtitles";
    } else if (mediaType === "EPISODE") {
        subtitleUrl =
            "/api/shows/episodes/" +
            encodeURIComponent(mediaId) +
            "/subtitles";
    } else {
        return;
    }

    try {
        const response =
            await fetch(
                subtitleUrl,
                {
                    method: "HEAD"
                }
            );

        if (version !== subtitleLoadVersion) {
            return;
        }

        if (response.status === 404) {
            appendConsoleMessage(
                "No subtitles found for the current media.",
                "info"
            );

            return;
        }

        if (!response.ok) {
            throw new Error(
                "Subtitle request failed: HTTP " +
                response.status
            );
        }

        const trackElement =
            document.createElement("track");

        trackElement.kind =
            "subtitles";

        trackElement.label =
            "Subtitles";

        trackElement.srclang =
            "und";

        trackElement.default =
            true;

        trackElement.src =
            subtitleUrl;

        trackElement.addEventListener(
            "error",
            () => {
                if (
                    version === subtitleLoadVersion
                ) {
                    appendConsoleMessage(
                        "Could not load subtitles.",
                        "error"
                    );

                    console.error(
                        "Could not load subtitles:",
                        subtitleUrl
                    );
                }
            }
        );

        player.appendChild(trackElement);

        trackElement.track.mode =
            "showing";

        appendConsoleMessage(
            "Subtitles loaded.",
            "success"
        );
    } catch (error) {
        if (version === subtitleLoadVersion) {
            appendConsoleMessage(
                "Could not load subtitles.",
                "error"
            );

            console.error(
                "Could not load subtitles:",
                error
            );
        }
    }
}


async function restoreCurrentProgress(
    shouldStartPlaying
) {
    const userId = selectedUserId;
    const mediaType = currentMediaType;
    const mediaId = currentMediaId;

    let resumePosition = 0;

    try {
        resumePosition =
            await getResumePosition(
                userId,
                mediaType,
                mediaId
            );
    } catch (error) {
        setUserStatus(
            "The video can play, but saved " +
            "progress could not be loaded.",
            "error"
        );

        console.error(error);
    }

    if (
        userId !== selectedUserId ||
        mediaType !== currentMediaType ||
        mediaId !== currentMediaId
    ) {
        return;
    }

    const applyProgress = () => {
        if (
            userId !== selectedUserId ||
            mediaType !== currentMediaType ||
            mediaId !== currentMediaId
        ) {
            return;
        }

        let safePosition =
            resumePosition;

        if (
            Number.isFinite(player.duration) &&
            safePosition >= player.duration - 5
        ) {
            safePosition = 0;
        }

        player.currentTime =
            safePosition;

        if (userId === null) {
            setUserStatus(
                "Playing without a profile. " +
                "Progress will not be saved."
            );
        } else if (safePosition > 0) {
            setUserStatus(
                "Resuming " +
                getSelectedUserName() +
                " at " +
                formatTime(safePosition) +
                ".",
                "success"
            );
        }

        if (shouldStartPlaying) {
            player.play().catch(() => {
                // Browser may require manual play.
            });
        }
    };

    if (player.readyState >= 1) {
        applyProgress();
    } else {
        player.addEventListener(
            "loadedmetadata",
            applyProgress,
            { once: true }
        );
    }
}


async function getResumePosition(
    userId,
    mediaType,
    mediaId
) {
    if (
        userId === null ||
        mediaType === null ||
        mediaId === null
    ) {
        return 0;
    }

    const response =
        await fetch(
            progressUrl(
                userId,
                mediaType,
                mediaId
            )
        );

    if (
        response.status === 404 ||
        response.status === 204
    ) {
        return 0;
    }

    if (!response.ok) {
        throw new Error(
            "Progress request failed."
        );
    }

    const progress =
        await response.json();

    if (progress.completed) {
        return 0;
    }

    const position =
        Number(progress.positionSeconds);

    return Number.isFinite(position) &&
        position > 0
        ? position
        : 0;
}


async function saveCurrentProgress(
    keepalive = false,
    forceSave = false
) {
    if (
        selectedUserId === null ||
        currentMediaType === null ||
        currentMediaId === null ||
        !hasPlaybackStarted
    ) {
        return;
    }

    const positionSeconds =
        Math.floor(player.currentTime);

    const durationSeconds =
        Math.floor(player.duration);

    if (
        !Number.isFinite(positionSeconds) ||
        !Number.isFinite(durationSeconds) ||
        durationSeconds <= 0
    ) {
        return;
    }

    if (
        !forceSave &&
        lastSavedPosition !== null &&
        Math.abs(
            positionSeconds -
            lastSavedPosition
        ) < 1
    ) {
        return;
    }

    const userId = selectedUserId;
    const mediaType = currentMediaType;
    const mediaId = currentMediaId;

    try {
        const response =
            await fetch(
                progressUrl(
                    userId,
                    mediaType,
                    mediaId
                ),
                {
                    method: "PUT",
                    headers: {
                        "Content-Type":
                            "application/json"
                    },
                    body: JSON.stringify({
                        positionSeconds,
                        durationSeconds
                    }),
                    keepalive
                }
            );

        if (!response.ok) {
            throw new Error(
                "Save-progress request failed."
            );
        }

        if (
            userId === selectedUserId &&
            mediaType === currentMediaType &&
            mediaId === currentMediaId
        ) {
            lastSavedPosition =
                positionSeconds;
        }
    } catch (error) {
        if (!keepalive) {
            setUserStatus(
                "Could not save watch progress.",
                "error"
            );

            console.error(error);
        }
    }
}


function progressUrl(
    userId,
    mediaType,
    mediaId
) {
    return (
        "/api/progress/" +
        encodeURIComponent(userId) +
        "/" +
        encodeURIComponent(mediaType) +
        "/" +
        encodeURIComponent(mediaId)
    );
}


function formatTime(totalSeconds) {
    const seconds =
        Math.floor(totalSeconds % 60);

    const minutes =
        Math.floor(
            (totalSeconds / 60) % 60
        );

    const hours =
        Math.floor(totalSeconds / 3600);

    if (hours > 0) {
        return (
            hours +
            ":" +
            String(minutes).padStart(2, "0") +
            ":" +
            String(seconds).padStart(2, "0")
        );
    }

    return (
        minutes +
        ":" +
        String(seconds).padStart(2, "0")
    );
}


loadUsers();
loadMovies();
loadShows();
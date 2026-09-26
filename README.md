# Local Media Server

Stream movies and TV shows from a Windows PC to a browser on your local network. Create profiles, watch with subtitles, and pick up where you left off.

**Java 21 · Spring Boot · SQLite · Docker Compose · HTML / CSS / JavaScript**

## Features

- **Movies and TV shows** — browse and play your MP4 library in a browser.
- **Subtitles** — load matching WebVTT (`.vtt`) files alongside your videos.
- **Profiles and playback progress** — save each profile's watch progress in SQLite.
- **Docker deployment** — run with Compose, persistent application data, and read-only media mounts.
- **Optional remote access** — connect through your private Tailscale network.

## Requirements

- Windows with PowerShell and a **Java 21 JDK**.
- **Docker Desktop**, running with Linux containers, for the Docker setup.
- Your own MP4 media files; Git if cloning from the command line.

The Maven wrapper is included. SQLite needs no separate installation or database server.

## Quick start with Docker

### 1. Get the project

```powershell
git clone https://github.com/yudesh-s/local-media-server.git
cd local-media-server
```

Already have the project? Open PowerShell in that folder. Run the remaining commands from the project root.

### 2. Prepare your folders

```powershell
New-Item -ItemType Directory -Force -Path ".\data", "C:\media-library\movies", "C:\media-library\shows"
```

Add your media using this layout:

| Media | Example path |
| --- | --- |
| Movie | `C:\media-library\movies\Example Movie.mp4` |
| TV episode | `C:\media-library\shows\Example Show\S01E01 - Pilot.mp4` |
| Episode subtitles | `C:\media-library\shows\Example Show\S01E01 - Pilot.vtt` |

Subtitles are optional. Place each `.vtt` beside its video with the same base filename; the same rule applies to movies.

For different media locations, edit the host `source` paths in `compose.yaml`. The source folders must exist before startup. Docker mounts the media folders read-only.

### 3. Build and launch

Build the application JAR:

```powershell
.\mvnw.cmd clean package
```

Once Maven reports **BUILD SUCCESS**, build the image and start the container:

```powershell
docker compose up --build -d
```

The Dockerfile packages the JAR from `target/`, so the Maven build must come first.

### 4. Open the app

| Connection | Browser address |
| --- | --- |
| On the server PC | [http://localhost:8080](http://localhost:8080) |
| From another device on the LAN | `http://SERVER_LAN_IP:8080` |
| Through Tailscale | `http://SERVER_TAILSCALE_IP:8080` |

Replace the IP placeholders with your server PC's addresses. For Tailscale access, connect both devices to your tailnet. If a LAN device cannot connect, check that Windows Firewall permits inbound TCP port 8080 on your private network.

Create a profile, choose a video, and start watching. Profiles have no passwords; this app is intended for a trusted LAN or private Tailscale network.

## Data and persistence

SQLite stores profiles and watch progress in **`data/media-server.db`**. Docker mounts the local `data/` folder at `/app/data`, keeping saved progress when the container is recreated or rebuilt.

- A fresh checkout starts with no profiles; the database is initialized at startup.
- Keep `data/` when updating. To back it up, stop the app and copy the folder.
- Keep databases, backups, media, and `target/` build output out of Git.

## Everyday commands

| Action | Command |
| --- | --- |
| Stop the app | `docker compose stop media-server` |
| Start it again | `docker compose up -d` |
| Follow application logs | `docker compose logs -f media-server` |

After changing application code, run these in order. Continue to the Docker build only after Maven succeeds:

```powershell
docker compose stop media-server
.\mvnw.cmd clean package
docker compose up --build -d
```

## Run directly with Java

Create the folders from step 2 and set your media paths in `src/main/resources/application.properties`. Keep the existing SQLite settings.

If the Docker version is running, stop it with `docker compose stop media-server`. Then start the native app:

```powershell
.\mvnw.cmd spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). Press `Ctrl+C` to stop.

Run one instance at a time: the native app and Docker share port 8080 and the same local SQLite database.


## System design

See the [project overview and system design](PROJECT_OVERVIEW.md)
for architecture diagrams, request flows, and database design.

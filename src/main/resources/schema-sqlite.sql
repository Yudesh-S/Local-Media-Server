CREATE TABLE IF NOT EXISTS user_profiles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS watch_progress (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    media_id VARCHAR(36) NOT NULL,
    position_seconds REAL NOT NULL,
    duration_seconds REAL NOT NULL,
    completed BOOLEAN NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT unique_user_media_progress
        UNIQUE (user_id, media_type, media_id),

    CONSTRAINT fk_watch_progress_user
        FOREIGN KEY (user_id)
        REFERENCES user_profiles(id)
);
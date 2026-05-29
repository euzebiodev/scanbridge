CREATE TABLE IF NOT EXISTS users (
    username TEXT PRIMARY KEY,
    password TEXT NOT NULL,
    enabled INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS authorities (
    username TEXT NOT NULL,
    authority TEXT NOT NULL,
    UNIQUE(username, authority),
    FOREIGN KEY(username) REFERENCES users(username)
);

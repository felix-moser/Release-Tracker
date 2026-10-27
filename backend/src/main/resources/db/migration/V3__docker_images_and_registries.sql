-- Foreign keys are enabled per connection by Hikari connection-init-sql.
CREATE TABLE docker_images (
    image_name TEXT PRIMARY KEY,
    manifest_digest TEXT NOT NULL,
    template_name TEXT,
    repo_name TEXT UNIQUE REFERENCES releases(repo) ON UPDATE CASCADE ON DELETE CASCADE
);
CREATE TABLE registry_credentials (
    registry TEXT PRIMARY KEY,
    username TEXT NOT NULL,
    token TEXT NOT NULL
);

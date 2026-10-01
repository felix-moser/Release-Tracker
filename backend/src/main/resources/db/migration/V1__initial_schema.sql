CREATE TABLE releases (
                          repo TEXT NOT NULL,
                          repo_url TEXT NOT NULL,
                          latest_tag TEXT NOT NULL,
                          template_name TEXT
);

CREATE UNIQUE INDEX idx_releases_repo
    ON releases (repo);

CREATE UNIQUE INDEX idx_releases_repo_url
    ON releases (repo_url);

CREATE TABLE secrets (
                         key TEXT PRIMARY KEY,
                         value TEXT NOT NULL
);

CREATE TABLE ticket_templates(
                                 id              INTEGER PRIMARY KEY AUTOINCREMENT,
                                 name            TEXT UNIQUE NOT NULL,
                                 project_key     TEXT NOT NULL,
                                 issue_type      TEXT NOT NULL,
                                 summary         TEXT NOT NULL,
                                 description     TEXT,
                                 assignee_id     TEXT,
                                 priority        TEXT,
                                 due_date        TEXT,
                                 labels          TEXT,
                                 transition_name TEXT
);
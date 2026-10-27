# Docker Image Tracking Specification

## Overview
Add support for tracking Docker container images (standalone or linked 1-to-1 to a tracked GitHub repository) using Jib Core (`com.google.cloud.tools:jib-core`) in the Spring Boot backend, storing only the normalized image name (without tag) and the `:latest` manifest digest, authenticating against multiple container registries, and creating Jira tickets via ticket templates.

See `GLOSSARY.md` and `docs/adr/0001-docker-image-tracking.md`.

## Requirements

### 1. Database Schema & Migration (`V3__docker_images_and_registries.sql`)
- Enable SQLite foreign keys (`PRAGMA foreign_keys = ON;`) on connections so `ON UPDATE CASCADE` and `ON DELETE CASCADE` work reliably.
- Create `docker_images` table:
  ```sql
  CREATE TABLE docker_images (
      image_name      TEXT PRIMARY KEY,
      manifest_digest TEXT NOT NULL,
      template_name   TEXT,
      repo_name       TEXT UNIQUE REFERENCES releases(repo) ON UPDATE CASCADE ON DELETE CASCADE
  );
  ```
- Create `registry_credentials` table:
  ```sql
  CREATE TABLE registry_credentials (
      registry TEXT PRIMARY KEY,
      username TEXT NOT NULL,
      token    TEXT NOT NULL
  );
  ```

### 2. Jib Core Integration (`DockerRegistryClient`)
- Add `com.google.cloud.tools:jib-core` to `backend/pom.xml`.
- Parse any user-provided Docker image input with Jib Core (`ImageReference.parse(...)`).
- **Tag stripping ("always use `:latest`")**: Strip any `:tag` or `@sha256:...` from the input so the stored `image_name` has no tag (e.g., `nginx:1.25` -> `nginx`, `ghcr.io/owner/app:v1` -> `ghcr.io/owner/app`). Always resolve and query the `"latest"` tag when talking to the registry.
- **Registry credential matching**:
  - Normalize Docker Hub aliases (`docker.io`, `index.docker.io`, `registry-1.docker.io`, `hub.docker.com`) so a credential saved for `docker.io` matches Jib's `registry-1.docker.io` / `index.docker.io`.
  - Match other registries by normalized host (e.g., `ghcr.io`, `quay.io`, custom host:port).
  - Authenticate via Jib's `RegistryClient` (Bearer auth with Basic credentials / Basic auth fallback, or anonymous Bearer auth if no credentials exist for that registry).
- **Manifest digest retrieval**:
  - Fetch the top-level `:latest` manifest/manifest-list digest (`sha256:...`) via Jib Core's `RegistryClient` (`checkManifest("latest")` / `pullManifest("latest")`).

### 3. Backend API & Services
- **Repositories API (`/api/repos`)**:
  - Extend `RepoRequest` with optional `docker_image` (`@JsonProperty("docker_image")`) and `docker_template` (`@JsonProperty("docker_template")`).
  - Extend `RepositoryResponse` with `docker_image`, `docker_digest`, and `docker_template` (`@JsonProperty` snake_case).
  - On `createRepo`: if `docker_image` is provided and non-blank, strip any tag, fetch `:latest` manifest digest via `DockerRegistryClient`, and insert into `docker_images` with `repo_name = repo.name()`.
  - On `updateRepo`:
    - Rename cascades via FK (`ON UPDATE CASCADE`) or explicit update.
    - If `docker_image` is provided and non-blank, strip any tag, fetch `:latest` manifest digest, and upsert the linked row in `docker_images` for `repo_name`.
    - If `docker_image` is null or blank, remove any linked row in `docker_images` where `repo_name = repo`.
  - On `removeRepo`: deleting the repository deletes its linked `docker_images` row (`ON DELETE CASCADE` / cleanup).
- **Standalone Docker Images API (`/api/docker-images`)**:
  - `GET /api/docker-images`: list standalone Docker images (`WHERE repo_name IS NULL`), returning `image_name`, `manifest_digest`, `template_name`.
  - `POST /api/docker-images`: accept `image_name` (or `image`) and optional `template` (`template_name`). Strip any tag, fetch `:latest` manifest digest, insert into `docker_images` with `repo_name = NULL`. Return 409 Conflict if `image_name` already exists (either standalone or linked).
  - `PUT /api/docker-images`: update a standalone Docker image (identified by original `image_name`, e.g. query param or path/body), updating `image_name`, `manifest_digest` (re-fetched for `:latest`), and `template_name`.
  - `DELETE /api/docker-images`: delete a standalone Docker image by `image_name`.
- **Registry Credentials API (`/api/system/registries`)**:
  - `GET /api/system/registries`: return list of configured registries (`registry`, `username`, `token_set: true` — do not expose raw `token`).
  - `POST /api/system/registries`: save/upsert a registry credential (`registry`, `username`, `token`). If updating an existing registry and `token` is blank/omitted, keep existing token if updating username, or require token on create.
  - `DELETE /api/system/registries`: delete a registry credential by `registry`.
- **Jira Ticket Placeholders (`JiraService`)**:
  - Single `ticket_templates` table unchanged.
  - Placeholders supported in `summary` and `description`: `{repo}`, `{tag}`, `{url}`, `{image}`, `{digest}`.
  - **GitHub release ticket**: fill `{repo}`, `{tag}`, `{url}`; replace `{image}` and `{digest}` with `" "` (a single space).
  - **Docker image ticket**: fill `{image}` (the stored `image_name`), `{tag}` (`"latest"`), `{digest}` (12-char short digest: first 12 hex chars after `sha256:`); replace `{repo}` and `{url}` with `" "` (a single space).
- **Check Cycle (`RepoService.checkAllRepos()` & `SchedulerService`)**:
  - Check all tracked GitHub repositories and their linked Docker images, plus all standalone Docker images.
  - **For a GitHub repository**:
    - Check if GitHub release qualifies (`repoChanged`).
    - If the repository has a linked Docker image, fetch the `:latest` manifest digest and check if it differs from stored `manifest_digest` (`dockerChanged`).
    - **If both `repoChanged` and `dockerChanged`**:
      - Create **only** the GitHub Jira ticket (using the repository's `template_name` and GitHub placeholders `{repo}`, `{tag}`, `{url}`, with `{image}` and `{digest}` replaced by `" "`).
      - Update **both** `releases.latest_tag` and `docker_images.manifest_digest` in the database.
    - **If only `repoChanged`**:
      - Create the GitHub Jira ticket and update `releases.latest_tag`.
    - **If only `dockerChanged`**:
      - Create a Docker Jira ticket using the linked Docker image's own `template_name` (and Docker placeholders `{image}`, `{tag}`, `{digest}`, with `{repo}` and `{url}` replaced by `" "`), and update `docker_images.manifest_digest`.
  - **For standalone Docker images (`repo_name IS NULL`)**:
    - Fetch `:latest` manifest digest and compare against stored `manifest_digest`.
    - If changed, create a Docker Jira ticket using the standalone image's `template_name` and update `docker_images.manifest_digest`.
    - Include standalone Docker image check results in `/api/repos/check` results (or run them in the check cycle and include their results).

### 4. Frontend Changes
- **`frontend/src/api.js`**:
  - Add API methods for `/api/docker-images` (GET, POST, PUT, DELETE) and `/api/system/registries` (GET, POST, DELETE).
- **`RepoModal.jsx` & `RepositoriesView.jsx` & `DashboardView.jsx`**:
  - In the Add/Edit Repository modal, include an optional **Docker Image** section with:
    - Docker Image input (e.g. `nginx` or `ghcr.io/owner/app`, noting `:latest` is always tracked).
    - Docker Jira Template dropdown.
  - Display the linked Docker image (and short digest) on the repository row in `RepositoriesView` and card in `DashboardView`.
  - In `RepositoriesView` (and `DashboardView`), add a separate **Docker Images** section/table below Tracked Repositories that shows **only standalone Docker images** (linked Docker images must NOT appear in this table), with an **Add Docker Image** button and modal to create/edit/remove standalone Docker images.
- **`TemplateModal.jsx` & `TemplatesView.jsx`**:
  - Update the Summary / Description placeholder hint to show `{repo}, {tag}, {url}, {image}, {digest}`.
- **`SettingsView.jsx`**:
  - Add a **Container Registry Credentials** card allowing the user to list, add/update, and delete multiple registry credentials (`registry`, `username`, `token`).

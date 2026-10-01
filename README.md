# Release Tracker 

A tool that monitors GitHub repositories for new releases and automates the creation of Jira Tickets based on customizable templates. So that you are always up to date!

## Features

- **Repository Monitoring**: Track multiple GitHub repositories for their latest release tags.
- **Custom Templates**: Define flexible blueprints for tickets (Task, Bug, Story) that include dynamic variables.
- **Dynamic Variables**: For creating the ticket you can use those variables: `{repo}`, `{tag}` and `{url}`
- **Automated Workflow**: Link a repository to a template to automate issue creation whenever a new version is detected.



## Getting Started

The easiest way to run it is using Docker. The included `docker-compose.yml` uses the latest pre-built image, which packages both the backend and frontend into a single container.

### Running with Docker (Recommended)

1. Make sure you have [Docker](https://docs.docker.com/get-docker/) installed.
2. Download the `docker-compose.yml` and run:

```bash
docker-compose up -d
```

3. The application will be available at `http://localhost:8080`.
4. Your SQLite database is in the `./data` folder in the project directory.

### Running Locally (For Development)

If you'd like to work on the codebase or run it locally without Docker:

**1. Start the Java Backend:**
Make sure you have JDK 21 installed.
```bash
cd backend
./mvnw spring-boot:run
```
The API will start on `http://localhost:8080`.

**2. Start the React Frontend:**
In a new terminal window:
```bash
cd frontend
npm install
npm run dev
```
The frontend will start on a local Vite development server (usually `http://localhost:5173`).

## Tech Stack

- **Frontend**: React, Vite
- **Backend**: Java (Spring Boot)
- **Database**: SQLite (local file)

## Project Structure

- `/frontend` - The React application codebase.
- `/backend` - The Spring Boot Java API.
- `docker-compose.yml` & `Dockerfile` - Containerization configurations.

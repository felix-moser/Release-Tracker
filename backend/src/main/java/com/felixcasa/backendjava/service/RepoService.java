package com.felixcasa.backendjava.service;

import com.felixcasa.backendjava.client.GithubClient;
import com.felixcasa.backendjava.client.DockerRegistryClient;
import com.felixcasa.backendjava.repository.DockerImageRepository;
import org.springframework.transaction.annotation.Transactional;
import com.felixcasa.backendjava.exception.RepoNotFoundException;
import com.felixcasa.backendjava.repository.RepoRepository;
import com.felixcasa.backendjava.request.RepoRequest;
import com.felixcasa.backendjava.response.CheckRepoResultResponse;
import com.felixcasa.backendjava.response.IsLatestVersionResponse;
import com.felixcasa.backendjava.response.RepositoryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
public class RepoService {
    private final RepoRepository repoRepository;
    private final GithubClient githubClient;
    private final SystemService systemService;
    private final JiraService jiraService;
    private final DockerRegistryClient dockerRegistryClient;
    private final DockerImageRepository dockerImages;
    private final Object checkCycleLock = new Object();
    private CompletableFuture<List<CheckRepoResultResponse>> activeCheckCycle;

    public RepoService(RepoRepository repoRepository, GithubClient githubClient, SystemService systemService, JiraService jiraService, DockerRegistryClient dockerRegistryClient, DockerImageRepository dockerImages) {
        this.repoRepository = repoRepository;
        this.githubClient = githubClient;
        this.systemService = systemService;
        this.jiraService = jiraService;
        this.dockerRegistryClient = dockerRegistryClient;
        this.dockerImages = dockerImages;
    }

    @Transactional
    public RepositoryResponse createRepo(RepoRequest repoRequest) {
        String correctUrl = getReleaseApiUrl(repoRequest.githubUrl());

        String latestRelease;
        try {
            latestRelease = githubClient.getRepository(correctUrl,
                    systemService.getSecret("GITHUB_API_KEY"));
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Network error while adding repository: " + e.getMessage(),
                    e
            );
        }

        boolean success = repoRepository.addNewRepo(
                repoRequest.name(),
                correctUrl,
                latestRelease,
                repoRequest.template(),
                repoRequest.versionComparison()
        );

        if (!success) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Repository '" + repoRequest.name() + "' already exists or failed to add."
            );
        }

        saveLinkedImage(repoRequest);
        return listRepos().stream()
                .filter(r -> r.repo().equals(repoRequest.name()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Repository was added but could not be retrieved."
                ));
    }

    public Map<String, String> removeRepo(String repoName) {
        if (repoRepository.removeRepo(repoName)) {
            return Map.of("message", "Repository '" + repoName + "' removed successfully.");
        } else {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Repository '" + repoName + "' not found."
            );
        }

    }

    public List<RepositoryResponse> listRepos() {
        return repoRepository.listRepos();
    }

    @Transactional
    public RepositoryResponse updateRepo(String repoName, RepoRequest repoRequest) {
        String correctUrl = getReleaseApiUrl(repoRequest.githubUrl());

        String latestRelease;
        try {
            latestRelease = githubClient.getRepository(correctUrl,
                    systemService.getSecret("GITHUB_API_KEY"));
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Network error while updating repository: " + e.getMessage(),
                    e
            );
        }
        boolean success = repoRepository.updateRepo(
                repoName,
                repoRequest.name(),
                correctUrl,
                latestRelease,
                repoRequest.template(),
                repoRequest.versionComparison()
        );

        if (!success) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Repository update failed (e.g., name conflict or '" + repoRequest.name() + "' not found)."
            );
        }

        saveLinkedImage(repoRequest);
        return listRepos().stream()
                .filter(r -> r.repo().equals(repoRequest.name()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Repository was updated but could not be retrieved."
                ));
    }

    private void saveLinkedImage(RepoRequest request) {
        String input = request.dockerImage();
        if (input == null || input.isBlank()) {
            dockerImages.deleteLinked(request.name());
            return;
        }
        String image = DockerRegistryClient.normalizeImage(input);
        String digest = dockerRegistryClient.fetchLatestDigest(image);
        dockerImages.deleteLinked(request.name());
        dockerImages.insert(new DockerImageRepository.TrackedImage(image, digest, request.dockerTemplate()), request.name());
    }

    public String getReleaseApiUrl(String githubUrl) {
        URI uri = URI.create(githubUrl);
        String path = uri.getPath().replaceAll("^/+|/+$",
                "");
        String[] parts = path.split("/");

        if ("api.github.com".equalsIgnoreCase(uri.getHost()) && parts.length == 5
                && "repos".equals(parts[0]) && "releases".equals(parts[3]) && "latest".equals(parts[4])) {
            parts = new String[]{parts[1], parts[2]};
        }

        if (parts.length != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid Github repository URL: "+ githubUrl);
        }

        String owner = parts[0];
        String repo = parts[1];

        if (repo.endsWith(".git")) {
            repo = repo.substring(0, repo.length() - 4);
        }

        return String.format("https://api.github.com/repos/%s/%s/releases/latest", owner, repo);
    }

    public IsLatestVersionResponse isLatestVersion(String repoName){

        RepositoryResponse repositoryResponse = repoRepository.listRepo(repoName).orElse(null);
        if (repositoryResponse == null){
            throw new RepoNotFoundException("Invalid Github repository "+ repoName);
        }

        String currentRelease = null;
        String digest = null;
        RuntimeException fetchFailure = null;
        String image = repositoryResponse.dockerImage();
        try {
            currentRelease = githubClient.getRepository(repositoryResponse.repoUrl(), systemService.getSecret("GITHUB_API_KEY"));
        } catch (RuntimeException e) {
            if (image == null) throw e;
            fetchFailure = e;
        }
        boolean repoChanged = currentRelease != null && repositoryResponse.versionComparison().qualifies(
                currentRelease, repoRepository.getLatestWrittenRelease(repoName).orElse(null));
        if (image != null) {
            try {
                digest = dockerRegistryClient.fetchLatestDigest(image);
            } catch (RuntimeException e) {
                if (fetchFailure == null) fetchFailure = e;
                else if (fetchFailure != e) fetchFailure.addSuppressed(e);
            }
        }
        boolean dockerChanged = digest != null && !Objects.equals(digest, repositoryResponse.dockerDigest());
        String jiraTicket = null;
        if (repoChanged) {
            // GitHub takes ticket precedence when both sources change. Advance neither state if its ticket fails.
            if (hasTemplate(repositoryResponse.templateName())) {
                jiraTicket = jiraService.createJiraTicket(repositoryResponse.templateName(), repoName, currentRelease, repositoryResponse.repoUrl());
            }
            repoRepository.writeLatestRelease(repoName, currentRelease);
            if (dockerChanged) dockerImages.writeDigest(image, digest);
        } else if (dockerChanged) {
            jiraTicket = processDockerDigestChange(image, repositoryResponse.dockerDigest(), digest, repositoryResponse.dockerTemplate()).jiraTicket();
        } else if (fetchFailure != null) {
            throw fetchFailure;
        }
        return new IsLatestVersionResponse(repoChanged || dockerChanged, jiraTicket);
    }

    private IsLatestVersionResponse processDockerDigestChange(String image, String storedDigest, String digest, String template) {
        boolean changed = !Objects.equals(digest, storedDigest);
        String ticket = null;
        if (changed) {
            if (hasTemplate(template)) ticket = jiraService.createDockerJiraTicket(template, image, digest);
            dockerImages.writeDigest(image, digest);
        }
        return new IsLatestVersionResponse(changed, ticket);
    }

    private boolean hasTemplate(String template) { return template != null && !template.isBlank(); }

    public List<CheckRepoResultResponse> checkAllRepos() {
        CompletableFuture<List<CheckRepoResultResponse>> cycle;
        boolean startsCycle;
        synchronized (checkCycleLock) {
            startsCycle = activeCheckCycle == null;
            if (startsCycle) activeCheckCycle = new CompletableFuture<>();
            cycle = activeCheckCycle;
        }
        if (startsCycle) {
            try {
                List<CheckRepoResultResponse> results = runCheckCycle();
                synchronized (checkCycleLock) {
                    cycle.complete(results);
                    activeCheckCycle = null;
                }
            } catch (Throwable failure) {
                synchronized (checkCycleLock) {
                    cycle.completeExceptionally(failure);
                    activeCheckCycle = null;
                }
            }
        }
        try {
            return cycle.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException failure) throw failure;
            if (e.getCause() instanceof Error failure) throw failure;
            throw e;
        }
    }

    private List<CheckRepoResultResponse> runCheckCycle() {
        List<RepositoryResponse> repos = listRepos();

        var results = new java.util.ArrayList<>(repos.parallelStream().map(repo -> {
            try {
                IsLatestVersionResponse result = isLatestVersion(repo.repo());
                return new CheckRepoResultResponse(repo.repo(), result.updated(), result.jiraTicket(), null);
            } catch (Exception e) {
                return new CheckRepoResultResponse(repo.repo(), false, null, true);
            }
        }).toList());
        for (var image : dockerImages.listStandalone()) {
            try {
                String digest = dockerRegistryClient.fetchLatestDigest(image.imageName());
                var result = processDockerDigestChange(image.imageName(), image.manifestDigest(), digest, image.templateName());
                results.add(new CheckRepoResultResponse(image.imageName(), result.updated(), result.jiraTicket(), null));
            } catch (Exception e) {
                results.add(new CheckRepoResultResponse(image.imageName(), false, null, true));
            }
        }
        return results;
    }

}

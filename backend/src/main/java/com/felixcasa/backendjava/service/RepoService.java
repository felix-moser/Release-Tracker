package com.felixcasa.backendjava.service;

import com.felixcasa.backendjava.client.GithubClient;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RepoService {
    private final RepoRepository repoRepository;
    private final GithubClient githubClient;
    private final SystemService systemService;
    private final JiraService jiraService;

    public RepoService(RepoRepository repoRepository, GithubClient githubClient, SystemService systemService, JiraService jiraService) {
        this.repoRepository = repoRepository;
        this.githubClient = githubClient;
        this.systemService = systemService;
        this.jiraService = jiraService;
    }

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
                repoRequest.template()
        );

        if (!success) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Repository '" + repoRequest.name() + "' already exists or failed to add."
            );
        }

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
                repoRequest.template()
        );

        if (!success) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Repository update failed (e.g., name conflict or '" + repoRequest.name() + "' not found)."
            );
        }

        return listRepos().stream()
                .filter(r -> r.repo().equals(repoRequest.name()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Repository was updated but could not be retrieved."
                ));
    }

    public String getReleaseApiUrl(String githubUrl) {
        String path = URI.create(githubUrl).getPath().replaceAll("^/+|/+$",
                "");
        String[] parts = path.split("/");

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

        String currentRelease = githubClient.getRepository(repositoryResponse.repoUrl(), systemService.getSecret("GITHUB_API_KEY"));
        String currentWrittenRelease = repoRepository.getLatestWrittenRelease(repoName).orElse(null);

        //Reminder Change is Latest version to accept jira ticket
        if (Objects.equals(currentRelease, currentWrittenRelease)){
            return new IsLatestVersionResponse(false,null);
        } else {
            String jiraTicket = null;
            if (repositoryResponse.templateName() != null && !repositoryResponse.templateName().isEmpty()) {
                jiraTicket = jiraService.createJiraTicket(repositoryResponse.templateName(), repoName, currentRelease, repositoryResponse.repoUrl());
            }
            repoRepository.writeLatestRelease(repoName, currentRelease);
            return new IsLatestVersionResponse(true, jiraTicket);
        }
    }

    public List<CheckRepoResultResponse> checkAllRepos(){
        List<RepositoryResponse> repos = listRepos();

        return repos.parallelStream().map(repo -> {
            try {
                IsLatestVersionResponse result = isLatestVersion(repo.repo());
                return new CheckRepoResultResponse(repo.repo(), result.updated(), result.jiraTicket(), null);
            } catch (Exception e) {
                return new CheckRepoResultResponse(repo.repo(), false, null, true);
            }
        }).toList();
    }

}

package com.felixcasa.backendjava.service;

import com.felixcasa.backendjava.client.GithubClient;
import com.felixcasa.backendjava.repository.RepoRepository;
import com.felixcasa.backendjava.request.RepoRequest;
import com.felixcasa.backendjava.response.RepositoryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RepoServiceTest {

    @Mock
    private RepoRepository repoRepository;

    @Mock
    private GithubClient githubClient;

    @Mock
    private SystemService systemService;

    @Mock
    private JiraService jiraService;

    @InjectMocks
    private RepoService repoService;

    @Test
    void getReleaseApiUrlShouldReturnCorrectUrl(){
        String result = repoService.getReleaseApiUrl("https://github.com/siderolabs/talos");

        assertThat(result).contains("https://api.github.com/repos/siderolabs/talos/releases/latest");
    }

    @Test
    void  getReleaseApiUrlShouldThrowErrorWhenWrongUrl(){
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> repoService.getReleaseApiUrl("https://github.com/justowner")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Invalid Github repository URL"));
    }

    @Test
    void removeRepoShouldRemoveSuccessfully(){
        when(repoRepository.removeRepo("repo")).thenReturn(true);

        Map<String, String> result = repoService.removeRepo("repo");

        assertThat(result).containsEntry("message","Repository 'repo' removed successfully.");
    }

    @Test
    void removeRepoShouldThrowErrorWhenRepoGotNotRemoved(){
        when(repoRepository.removeRepo("repo")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> repoService.removeRepo("repo")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Repository 'repo' not found."));
    }

    @Test
    void listRepoShouldReturnAllRepos(){
        List<RepositoryResponse> repoList = java.util.List.of(new RepositoryResponse("repo","url","tag","template"));
        when(repoRepository.listRepos()).thenReturn(repoList);

        List<RepositoryResponse> result = repoService.listRepos();

        assertThat(result).isEqualTo(repoList);
    }

    @Test
    void createRepoShouldReturnRepositoryResponseWhenSuccessful(){
        RepoRequest repoRequest = new RepoRequest("talos", "https://github.com/siderolabs/talos", "template");
        String apiUrl = "https://api.github.com/repos/siderolabs/talos/releases/latest";
        RepositoryResponse expected = new RepositoryResponse("talos", apiUrl, "v1.0.0", "template");

        when(systemService.getSecret("GITHUB_API_KEY")).thenReturn("api-key");
        when(githubClient.getRepository(apiUrl, "api-key")).thenReturn("v1.0.0");
        when(repoRepository.addNewRepo("talos", apiUrl, "v1.0.0", "template")).thenReturn(true);
        when(repoRepository.listRepos()).thenReturn(List.of(expected));

        RepositoryResponse result = repoService.createRepo(repoRequest);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void createRepoShouldThrowBadGatewayWhenNetworkErrorOccurs(){
        RepoRequest repoRequest = new RepoRequest("talos", "https://github.com/siderolabs/talos", "template");
        String apiUrl = "https://api.github.com/repos/siderolabs/talos/releases/latest";

        when(systemService.getSecret("GITHUB_API_KEY")).thenReturn("api-key");
        when(githubClient.getRepository(apiUrl, "api-key")).thenThrow(new RestClientException("connection refused"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> repoService.createRepo(repoRequest)
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Network error while adding repository"));
    }

    @Test
    void createRepoShouldThrowConflictWhenRepoAlreadyExists(){
        RepoRequest repoRequest = new RepoRequest("talos", "https://github.com/siderolabs/talos", "template");
        String apiUrl = "https://api.github.com/repos/siderolabs/talos/releases/latest";

        when(systemService.getSecret("GITHUB_API_KEY")).thenReturn("api-key");
        when(githubClient.getRepository(apiUrl, "api-key")).thenReturn("v1.0.0");
        when(repoRepository.addNewRepo("talos", apiUrl, "v1.0.0", "template")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> repoService.createRepo(repoRequest)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("already exists or failed to add"));
    }

    @Test
    void updateRepoShouldReturnRepositoryResponseWhenSuccessful(){
        RepoRequest repoRequest = new RepoRequest("talos-new", "https://github.com/siderolabs/talos", "template");
        String apiUrl = "https://api.github.com/repos/siderolabs/talos/releases/latest";
        RepositoryResponse expected = new RepositoryResponse("talos-new", apiUrl, "v1.1.0", "template");

        when(systemService.getSecret("GITHUB_API_KEY")).thenReturn("api-key");
        when(githubClient.getRepository(apiUrl, "api-key")).thenReturn("v1.1.0");
        when(repoRepository.updateRepo("talos", "talos-new", apiUrl, "v1.1.0", "template")).thenReturn(true);
        when(repoRepository.listRepos()).thenReturn(List.of(expected));

        RepositoryResponse result = repoService.updateRepo("talos", repoRequest);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void updateRepoShouldThrowBadGatewayWhenNetworkErrorOccurs(){
        RepoRequest repoRequest = new RepoRequest("talos-new", "https://github.com/siderolabs/talos", "template");
        String apiUrl = "https://api.github.com/repos/siderolabs/talos/releases/latest";

        when(systemService.getSecret("GITHUB_API_KEY")).thenReturn("api-key");
        when(githubClient.getRepository(apiUrl, "api-key")).thenThrow(new RestClientException("connection refused"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> repoService.updateRepo("talos", repoRequest)
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Network error while updating repository"));
    }

    @Test
    void updateRepoShouldThrowConflictWhenUpdateFails(){
        RepoRequest repoRequest = new RepoRequest("talos-new", "https://github.com/siderolabs/talos", "template");
        String apiUrl = "https://api.github.com/repos/siderolabs/talos/releases/latest";

        when(systemService.getSecret("GITHUB_API_KEY")).thenReturn("api-key");
        when(githubClient.getRepository(apiUrl, "api-key")).thenReturn("v1.1.0");
        when(repoRepository.updateRepo("talos", "talos-new", apiUrl, "v1.1.0", "template")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> repoService.updateRepo("talos", repoRequest)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Repository update failed"));
    }

}


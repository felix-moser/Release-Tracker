package com.felixcasa.backendjava.service;

import com.felixcasa.backendjava.repository.SystemRepository;
import com.felixcasa.backendjava.request.SecretsRequest;
import com.felixcasa.backendjava.response.SecretsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SystemServiceTest {

    @Mock
    private SystemRepository systemRepository;

    @InjectMocks
    private SystemService systemService;

    @Test
    void saveSecretsShouldSaveNonBlankSecrets() {
        SecretsRequest request = new SecretsRequest(
                "https://example.atlassian.net",
                "example@example.com",
                "API-Token-Jira",
                "API-Key-Github"
        );

        Map<String, String> result = systemService.saveSecrets(request);

        assertThat(result).containsEntry("message", "Secrets saved successfully");

        verify(systemRepository).setSecret("JIRA_URL", "https://example.atlassian.net");
        verify(systemRepository).setSecret("JIRA_EMAIL", "example@example.com");
        verify(systemRepository).setSecret("JIRA_API_TOKEN", "API-Token-Jira");
        verify(systemRepository).setSecret("GITHUB_API_KEY", "API-Key-Github");
    }

    @Test
    void saveSecretsShouldSkipBlankSecrets() {
        SecretsRequest request = new SecretsRequest(
                "https://example.atlassian.net",
                "",
                "",
                "API-Key-Github"
        );

        Map<String, String> result = systemService.saveSecrets(request);

        assertThat(result).containsEntry("message", "Secrets saved successfully");

        verify(systemRepository).setSecret("JIRA_URL", "https://example.atlassian.net");
        verify(systemRepository, never()).setSecret(eq("JIRA_EMAIL"), anyString());
        verify(systemRepository, never()).setSecret(eq("JIRA_API_TOKEN"), anyString());
        verify(systemRepository).setSecret("GITHUB_API_KEY", "API-Key-Github");
    }

    @Test
    public void getSecretsShouldReturnSecrets(){
        when(systemRepository.getSecret("JIRA_URL")).thenReturn(Optional.of("https://example.atlassian.net"));
        when(systemRepository.getSecret("JIRA_EMAIL")).thenReturn(Optional.of("example@example.com"));
        when(systemRepository.getSecret("JIRA_API_TOKEN")).thenReturn(Optional.of("API-Token-Jira"));
        when(systemRepository.getSecret("GITHUB_API_KEY")).thenReturn(Optional.empty());

        SecretsResponse response = systemService.getSecrets();

        assertThat(response.jiraUrl()).isEqualTo("https://example.atlassian.net");
        assertThat(response.jiraEmail()).isEqualTo("example@example.com");
        assertThat(response.jiraApiTokenConfigured()).isTrue();
        assertThat(response.githubApiKeyConfigured()).isFalse();
    }

    @Test
    public void checkOnBoardedShouldReturnTrue(){
        when(systemRepository.getSecret("JIRA_URL")).thenReturn(Optional.of("https://example.atlassian.net"));
        when(systemRepository.getSecret("JIRA_EMAIL")).thenReturn(Optional.of("example@example.com"));

        Map<String, Boolean> result = systemService.checkOnboarded();

        assertThat(result).containsEntry("onboarded",true);
    }

    @Test
    public void checkOnBoardedShouldReturnFalse(){
        when(systemRepository.getSecret("JIRA_URL")).thenReturn(Optional.of("https://example.atlassian.net"));
        when(systemRepository.getSecret("JIRA_EMAIL")).thenReturn(Optional.empty());

        Map<String, Boolean> result = systemService.checkOnboarded();

        assertThat(result).containsEntry("onboarded",false);
    }


}

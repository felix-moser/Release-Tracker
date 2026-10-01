package com.felixcasa.backendjava.service;

import com.felixcasa.backendjava.repository.SystemRepository;
import com.felixcasa.backendjava.request.SecretsRequest;
import com.felixcasa.backendjava.response.SecretsResponse;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SystemService {
    private final SystemRepository systemRepository;

    public SystemService(SystemRepository systemRepository){
        this.systemRepository = systemRepository;
    }

    public Map<String, String> saveSecrets(SecretsRequest secretsRequest){
        if (secretsRequest.jiraUrl() != null && !secretsRequest.jiraUrl().isBlank()){
            systemRepository.setSecret("JIRA_URL",secretsRequest.jiraUrl());
        }
        if (secretsRequest.jiraEmail() != null && !secretsRequest.jiraEmail().isBlank()){
            systemRepository.setSecret("JIRA_EMAIL",secretsRequest.jiraEmail());
        }
        if (secretsRequest.jiraApiToken() != null && !secretsRequest.jiraApiToken().isBlank()){
            systemRepository.setSecret("JIRA_API_TOKEN",secretsRequest.jiraApiToken());
        }
        if (secretsRequest.githubApiKey() != null && !secretsRequest.githubApiKey().isBlank()){
            systemRepository.setSecret("GITHUB_API_KEY",secretsRequest.githubApiKey());
        }
        return Map.of("message","Secrets saved successfully");
    }

    public SecretsResponse getSecrets(){
        return new SecretsResponse(
                systemRepository.getSecret("JIRA_URL").orElse(null),
                systemRepository.getSecret("JIRA_EMAIL").orElse(null),
                systemRepository.getSecret("JIRA_API_TOKEN").isPresent(),
                systemRepository.getSecret("GITHUB_API_KEY").isPresent()
        );
    }

    public Map<String, Boolean> checkOnboarded(){
        if (systemRepository.getSecret("JIRA_URL").isPresent() && systemRepository.getSecret("JIRA_EMAIL").isPresent()){
            return Map.of("onboarded", Boolean.TRUE);
        } else{
            return Map.of("onboarded", Boolean.FALSE);
        }
    }

    public String getSecret(String secret){
        return systemRepository.getSecret(secret).orElse(null);
    }

    public void setSecret(String key, String value) {
        systemRepository.setSecret(key, value);
    }
}

package com.felixcasa.backendjava.client;

import com.felixcasa.backendjava.response.GithubReleaseResponse;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GithubClient {

    private final RestClient restClient;

    public GithubClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version","2026-03-10")
                .build();
    }

    public String getRepository(@NotEmpty String repoUrl, String apiKey){
        var request = restClient.get().uri(repoUrl);

        if (apiKey != null && !apiKey.isEmpty()) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }

        GithubReleaseResponse response = request
                .retrieve()
                .body(GithubReleaseResponse.class);

        return response != null? response.tagName(): null;
    }
}

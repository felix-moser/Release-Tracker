package com.felixcasa.backendjava.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public record RepositoryResponse(
        String repo,
        @JsonProperty("repo_url") String repoUrl,
        @JsonProperty("latest_tag") String latestTag,
        @JsonProperty("template_name") String templateName
) {
}

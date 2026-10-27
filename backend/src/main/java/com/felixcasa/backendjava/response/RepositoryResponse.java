package com.felixcasa.backendjava.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.felixcasa.backendjava.model.VersionComparisonMode;

public record RepositoryResponse(
        String repo,
        @JsonProperty("repo_url") String repoUrl,
        @JsonProperty("latest_tag") String latestTag,
        @JsonProperty("template_name") String templateName,
        @JsonProperty("version_comparison") VersionComparisonMode versionComparison
) {
}

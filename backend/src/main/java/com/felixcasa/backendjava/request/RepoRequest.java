package com.felixcasa.backendjava.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.felixcasa.backendjava.model.VersionComparisonMode;
import jakarta.validation.constraints.NotEmpty;

public record RepoRequest(
        @NotEmpty String name,
        @NotEmpty @JsonProperty("github_url") String githubUrl,
        String template,
        @JsonProperty("version_comparison") VersionComparisonMode versionComparison
) {
    public RepoRequest {
        if (versionComparison == null) {
            versionComparison = VersionComparisonMode.ANY_DIFFERENT;
        }
    }
}

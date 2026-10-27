package com.felixcasa.backendjava.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.felixcasa.backendjava.model.VersionComparisonMode;

public record RepositoryResponse(
        String repo,
        @JsonProperty("repo_url") String repoUrl,
        @JsonProperty("latest_tag") String latestTag,
        @JsonProperty("template_name") String templateName,
        @JsonProperty("version_comparison") VersionComparisonMode versionComparison,
        @JsonProperty("docker_image") String dockerImage,
        @JsonProperty("docker_digest") String dockerDigest,
        @JsonProperty("docker_template") String dockerTemplate
) {
    public RepositoryResponse(String repo, String url, String tag, String template, VersionComparisonMode mode) {
        this(repo, url, tag, template, mode, null, null, null);
    }
}

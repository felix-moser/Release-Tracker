package com.felixcasa.backendjava.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;

public record RepoRequest(
        @NotEmpty String name,
        @NotEmpty @JsonProperty("github_url") String githubUrl,
        String template
) {
}

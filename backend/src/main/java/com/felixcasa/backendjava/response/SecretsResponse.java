package com.felixcasa.backendjava.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SecretsResponse(
        @JsonProperty("jira_url") String jiraUrl,
        @JsonProperty("jira_email") String jiraEmail,
        @JsonProperty("jira_api_token") boolean jiraApiTokenConfigured,
        @JsonProperty("github_api_key") boolean githubApiKeyConfigured
) {
}

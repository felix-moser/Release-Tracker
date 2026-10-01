package com.felixcasa.backendjava.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SecretsRequest(
        @JsonProperty("jira_url") String jiraUrl,
        @JsonProperty("jira_email") String jiraEmail,
        @JsonProperty("jira_api_token") String jiraApiToken,
        @JsonProperty("github_api_key") String githubApiKey
) {
}

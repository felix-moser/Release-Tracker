package com.felixcasa.backendjava.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IsLatestVersionResponse(
        @JsonProperty("updated") boolean updated,
        @JsonProperty("jira_ticket") String jiraTicket
        ) {
}

package com.felixcasa.backendjava.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CheckRepoResultResponse(
        String repo,
        boolean updated,
        @JsonProperty("jira_ticket") String jiraTicket,
        Boolean error
) { }

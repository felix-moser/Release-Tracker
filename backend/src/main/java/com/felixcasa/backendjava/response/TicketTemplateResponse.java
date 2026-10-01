package com.felixcasa.backendjava.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TicketTemplateResponse(
    String name,
    @JsonProperty("project_key") String projectKey,
    @JsonProperty("issue_type") String issueType,
    String summary,
    String description,
    @JsonProperty("assignee_id") String assigneeId,
    String priority,
    @JsonProperty("due_date") String dueDate,
    String labels,
    @JsonProperty("transition_name") String transitionName
) {
}

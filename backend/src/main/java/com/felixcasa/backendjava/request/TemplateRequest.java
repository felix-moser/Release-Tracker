package com.felixcasa.backendjava.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;

public record TemplateRequest(
        @NotEmpty String name,
        @NotEmpty @JsonProperty("project_key") String projectKey,
        @NotEmpty @JsonProperty("issue_type") String issueType,
        @NotEmpty String summary,
        String description,
        @JsonProperty("assignee_id") String assigneeId,
        String priority,
        @JsonProperty("due_date") String dueDate,
        String labels,
        String transition
) {
}

package com.felixcasa.backendjava.request;

import jakarta.validation.constraints.NotEmpty;

public record TicketTemplateRequest(@NotEmpty String name,
                                    @NotEmpty String projectKey,
                                    @NotEmpty String issueType,
                                    @NotEmpty String summary,
                                    String description,
                                    String assigneeId,
                                    String priority,
                                    String dueDate,
                                    String labels,
                                    String transitionName
                                       ) {
}

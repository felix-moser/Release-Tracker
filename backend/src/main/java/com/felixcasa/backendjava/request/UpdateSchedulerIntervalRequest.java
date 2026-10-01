package com.felixcasa.backendjava.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;

public record UpdateSchedulerIntervalRequest(
        @Min(1) @JsonProperty("interval_minutes") int intervalMinutes
) {
}

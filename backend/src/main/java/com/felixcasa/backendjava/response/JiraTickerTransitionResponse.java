package com.felixcasa.backendjava.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraTickerTransitionResponse(@JsonProperty("transitions") List<Map<String, Object>> transitions) {
}

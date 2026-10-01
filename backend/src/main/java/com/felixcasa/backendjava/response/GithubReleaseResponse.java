package com.felixcasa.backendjava.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubReleaseResponse(@JsonProperty("tag_name") String tagName) {
}

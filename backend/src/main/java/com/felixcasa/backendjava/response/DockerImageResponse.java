package com.felixcasa.backendjava.response;
import com.fasterxml.jackson.annotation.JsonProperty;
public record DockerImageResponse(@JsonProperty("image_name") String imageName,
    @JsonProperty("manifest_digest") String manifestDigest,
    @JsonProperty("template_name") String templateName) {}

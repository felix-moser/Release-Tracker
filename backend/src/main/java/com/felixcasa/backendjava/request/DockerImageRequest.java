package com.felixcasa.backendjava.request;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
public record DockerImageRequest(@NotBlank @JsonProperty("image_name") @JsonAlias("image") String imageName,
    @JsonProperty("template_name") @JsonAlias("template") String templateName) {}

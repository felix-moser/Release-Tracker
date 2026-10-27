package com.felixcasa.backendjava.response;
import com.fasterxml.jackson.annotation.JsonProperty;
public record RegistryResponse(String registry, String username, @JsonProperty("token_set") boolean tokenSet) {}

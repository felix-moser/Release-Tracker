package com.felixcasa.backendjava.request;
import jakarta.validation.constraints.NotBlank;
public record RegistryRequest(@NotBlank String registry, @NotBlank String username, String token) {
    @Override public String toString() { return "RegistryRequest[registry=" + registry + ", token=REDACTED]"; }
}

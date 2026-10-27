package com.felixcasa.backendjava.client;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class DockerRegistryClientTest {
    private record RegistryClientMocks(com.google.cloud.tools.jib.registry.RegistryClient.Factory factory,
                                       com.google.cloud.tools.jib.registry.RegistryClient registry) {}

    private RegistryClientMocks registryClientMocks() {
        var factory = org.mockito.Mockito.mock(com.google.cloud.tools.jib.registry.RegistryClient.Factory.class);
        var registry = org.mockito.Mockito.mock(com.google.cloud.tools.jib.registry.RegistryClient.class);
        org.mockito.Mockito.when(factory.newRegistryClient()).thenReturn(registry);
        return new RegistryClientMocks(factory, registry);
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans = {true,false})
    void fetchesLatestTopLevelDigestWithMatchingHubCredential(boolean bearer) throws Exception {
        var credentials = org.mockito.Mockito.mock(com.felixcasa.backendjava.repository.RegistryRepository.class);
        org.mockito.Mockito.when(credentials.find("docker.io")).thenReturn(java.util.Optional.of(
                new com.felixcasa.backendjava.repository.RegistryRepository.RegistryCredential("alice","secret")));
        var fixture = registryClientMocks();
        var factory = fixture.factory();
        var registry = fixture.registry();
        org.mockito.Mockito.when(registry.doPullBearerAuth()).thenReturn(bearer);
        String digest = "sha256:" + "a".repeat(64);
        org.mockito.Mockito.when(registry.pullManifest("latest")).thenReturn(new com.google.cloud.tools.jib.registry.ManifestAndDigest<>(null,
                com.google.cloud.tools.jib.api.DescriptorDigest.fromDigest(digest)));
        try (var mocked = org.mockito.Mockito.mockStatic(com.google.cloud.tools.jib.registry.RegistryClient.class)) {
            mocked.when(() -> com.google.cloud.tools.jib.registry.RegistryClient.factory(
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("registry-1.docker.io"),
                    org.mockito.ArgumentMatchers.eq("library/nginx"), org.mockito.ArgumentMatchers.any())).thenReturn(factory);
            assertThat(new DockerRegistryClient(credentials).fetchLatestDigest("docker.io/nginx:old")).isEqualTo(digest);
            var captor = org.mockito.ArgumentCaptor.forClass(com.google.cloud.tools.jib.api.Credential.class);
            org.mockito.Mockito.verify(factory).setCredential(captor.capture());
            assertThat(captor.getValue().getUsername()).isEqualTo("alice");
            assertThat(captor.getValue().getPassword()).isEqualTo("secret");
            org.mockito.Mockito.verify(registry, org.mockito.Mockito.times(bearer ? 0 : 1)).configureBasicAuth();
            org.mockito.Mockito.verify(registry).pullManifest("latest");
        }
    }
    @Test void anonymousBearerAndRegistryFailuresDoNotExposeSecrets() throws Exception {
        var credentials = org.mockito.Mockito.mock(com.felixcasa.backendjava.repository.RegistryRepository.class);
        org.mockito.Mockito.when(credentials.find("ghcr.io")).thenReturn(java.util.Optional.empty());
        var fixture = registryClientMocks();
        var factory = fixture.factory();
        var registry = fixture.registry();
        org.mockito.Mockito.when(registry.doPullBearerAuth()).thenReturn(true);
        org.mockito.Mockito.when(registry.pullManifest("latest")).thenThrow(new java.io.IOException("secret-token"));
        try (var mocked = org.mockito.Mockito.mockStatic(com.google.cloud.tools.jib.registry.RegistryClient.class)) {
            mocked.when(() -> com.google.cloud.tools.jib.registry.RegistryClient.factory(
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("ghcr.io"),
                    org.mockito.ArgumentMatchers.eq("owner/app"), org.mockito.ArgumentMatchers.any())).thenReturn(factory);
            assertThatThrownBy(() -> new DockerRegistryClient(credentials).fetchLatestDigest("ghcr.io/owner/app:v1"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageNotContaining("secret-token");
            org.mockito.Mockito.verify(factory, org.mockito.Mockito.never()).setCredential(org.mockito.ArgumentMatchers.any());
            org.mockito.Mockito.verify(registry, org.mockito.Mockito.never()).configureBasicAuth();
        }
    }
    @ParameterizedTest @CsvSource({"nginx:1.25,nginx", "ghcr.io/owner/app:v1,ghcr.io/owner/app", "localhost:5000/app:old,localhost:5000/app", "docker.io/library/nginx:old,nginx", "index.docker.io/nginx,nginx"})
    void normalizesImagesAndStripsTags(String input, String expected) {
        assertThat(DockerRegistryClient.normalizeImage(input)).isEqualTo(expected);
    }
    @Test void stripsDigest() {
        assertThat(DockerRegistryClient.normalizeImage("nginx@sha256:" + "a".repeat(64))).isEqualTo("nginx");
    }
    @ParameterizedTest @CsvSource({"docker.io,docker.io", "index.docker.io,docker.io", "registry-1.docker.io,docker.io", "hub.docker.com,docker.io", "GHCR.IO,ghcr.io", "localhost:5000,localhost:5000"})
    void normalizesRegistryAliases(String input, String expected) {
        assertThat(DockerRegistryClient.normalizeRegistry(input)).isEqualTo(expected);
    }
    @Test void rejectsInvalidImages() {
        assertThatThrownBy(() -> DockerRegistryClient.normalizeImage("https://example.com/app")).isInstanceOf(RuntimeException.class);
    }
}

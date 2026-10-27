package com.felixcasa.backendjava.client;
import com.google.cloud.tools.jib.api.*;
import com.google.cloud.tools.jib.event.EventHandlers;
import com.google.cloud.tools.jib.http.FailoverHttpClient;
import com.google.cloud.tools.jib.registry.RegistryClient;
import com.felixcasa.backendjava.repository.RegistryRepository;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;
import java.util.Set;

@Component
public class DockerRegistryClient {
    private final RegistryRepository credentials;
    private static final Set<String> HUB = Set.of("docker.io", "index.docker.io", "registry-1.docker.io", "hub.docker.com");
    public DockerRegistryClient(RegistryRepository credentials) { this.credentials = credentials; }
    public static String normalizeRegistry(String input) {
        String host = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        if (!host.matches("[a-z0-9]+(?:[.-][a-z0-9]+)*(?::[0-9]+)?"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registry must be a host with an optional port");
        return HUB.contains(host) ? "docker.io" : host;
    }
    public static String normalizeImage(String input) {
        try {
            ImageReference ref = ImageReference.parse(input == null ? "" : input.trim());
            if (ref.isScratch()) throw new IllegalArgumentException();
            String host = normalizeRegistry(ref.getRegistry());
            String repository = ref.getRepository();
            if (host.equals("docker.io")) {
                if (repository.startsWith("library/")) repository = repository.substring(8);
                return repository;
            }
            return host + "/" + repository;
        } catch (InvalidImageReferenceException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Docker image reference");
        }
    }
    public String fetchLatestDigest(String image) {
        FailoverHttpClient http = new FailoverHttpClient(false, false, event -> {});
        try {
            ImageReference ref = ImageReference.parse(normalizeImage(image));
            var credential = credentials.find(normalizeRegistry(ref.getRegistry()));
            var factory = RegistryClient.factory(EventHandlers.NONE, ref.getRegistry(), ref.getRepository(), http);
            credential.ifPresent(c -> factory.setCredential(Credential.from(c.username(), c.token())));
            RegistryClient client = factory.newRegistryClient();
            if (!client.doPullBearerAuth() && credential.isPresent()) client.configureBasicAuth();
            return client.pullManifest("latest").getDigest().toString();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            // Registry errors can contain authentication responses; keep them out of API responses.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not fetch latest Docker manifest");
        } finally {
            try { http.shutDown(); } catch (java.io.IOException ignored) { }
        }
    }
}

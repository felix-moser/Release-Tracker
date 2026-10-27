package com.felixcasa.backendjava.controller;
import com.felixcasa.backendjava.client.DockerRegistryClient;
import com.felixcasa.backendjava.repository.RegistryRepository;
import com.felixcasa.backendjava.request.RegistryRequest;
import com.felixcasa.backendjava.response.RegistryResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController
@RequestMapping("/api/system/registries")
public class RegistryController {
    private final RegistryRepository repository;
    public RegistryController(RegistryRepository repository) { this.repository = repository; }
    @GetMapping public List<RegistryResponse> list() { return repository.list(); }
    @PostMapping public RegistryResponse save(@Valid @RequestBody RegistryRequest request) {
        String host = DockerRegistryClient.normalizeRegistry(request.registry());
        String token = request.token();
        if (token == null || token.isBlank()) token = repository.find(host).map(RegistryRepository.RegistryCredential::token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is required for a new registry"));
        repository.save(host, request.username(), token);
        return new RegistryResponse(host, request.username(), true);
    }
    @DeleteMapping public Map<String,String> delete(@RequestParam String registry) {
        if (!repository.delete(DockerRegistryClient.normalizeRegistry(registry)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registry not found");
        return Map.of("message", "Registry removed");
    }
}

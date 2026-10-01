package com.felixcasa.backendjava.controller;

import com.felixcasa.backendjava.request.RepoRequest;
import com.felixcasa.backendjava.response.CheckRepoResultResponse;
import com.felixcasa.backendjava.response.RepositoryResponse;
import com.felixcasa.backendjava.service.RepoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/repos")
public class RepoController {

    private final RepoService repoService;

    public RepoController(RepoService repoService){
        this.repoService = repoService;
    }

    @PostMapping
    public ResponseEntity<?> createRepo(@Valid @RequestBody RepoRequest repoRequest){
        try {
            RepositoryResponse response = repoService.createRepo(repoRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("detail", e.getReason()));
        }
    }

    @GetMapping
    public List<RepositoryResponse> listRepos(){
        return repoService.listRepos();
    }

    @PutMapping("/{name}")
    public ResponseEntity<?> updateRepo(@PathVariable String name, @Valid @RequestBody RepoRequest repoRequest){
        try {
            RepositoryResponse response = repoService.updateRepo(name, repoRequest);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("detail", e.getReason()));
        }
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<?> deleteRepo(@PathVariable String name){
        try {
            Map<String, String> response = repoService.removeRepo(name);
            return ResponseEntity.ok(Map.of("message", "Repository '" + name + "' removed."));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("detail", e.getReason()));
        }
    }

    @PostMapping("/check")
    public Map<String, Object> checkAllRepos(){
        List<CheckRepoResultResponse> results = repoService.checkAllRepos();
        return Map.of("checked", results.size(), "results", results);
    }
}

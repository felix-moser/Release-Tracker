package com.felixcasa.backendjava.controller;

import com.felixcasa.backendjava.request.TemplateRequest;
import com.felixcasa.backendjava.response.TicketTemplateResponse;
import com.felixcasa.backendjava.service.JiraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {
    private final JiraService jiraService;

    public TemplateController(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    @PostMapping
    public ResponseEntity<?> createTemplate(@Valid @RequestBody TemplateRequest templateRequest){
        boolean success = jiraService.addTemplate(templateRequest);
        if (success) {
            TicketTemplateResponse created = jiraService.getTemplate(templateRequest.name());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("detail", "Template '" + templateRequest.name() + "' already exists."));
        }
    }

    @GetMapping
    public List<TicketTemplateResponse> listTemplates(){
        return jiraService.listTemplates();
    }

    @GetMapping("/{name}")
    public ResponseEntity<?> getTemplate(@PathVariable String name){
        TicketTemplateResponse template = jiraService.getTemplate(name);
        if (template != null) {
            return ResponseEntity.ok(template);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "Template '" + name + "' not found."));
        }
    }

    @PutMapping("/{name}")
    public ResponseEntity<?> updateTemplate(@PathVariable String name, @Valid @RequestBody TemplateRequest templateRequest){
        TicketTemplateResponse existing = jiraService.getTemplate(name);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "Template '" + name + "' not found."));
        }
        boolean success = jiraService.updateTemplate(name, templateRequest);
        if (success) {
            TicketTemplateResponse updated = jiraService.getTemplate(templateRequest.name());
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("detail", "A template with the given name already exists or failed to update."));
        }
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<?> deleteTemplate(@PathVariable String name){
        TicketTemplateResponse existing = jiraService.getTemplate(name);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "Template '" + name + "' not found."));
        }
        boolean success = jiraService.deleteTemplate(name);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Template '" + name + "' deleted."));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "Template '" + name + "' not found."));
        }
    }

    @PostMapping("/{name}/test")
    public ResponseEntity<?> testTemplate(@PathVariable String name){
        try {
            String issueKey = jiraService.testTemplate(name);
            return ResponseEntity.ok(Map.of("message", "Test ticket created successfully: " + issueKey));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("detail", e.getReason()));
        }
    }
}

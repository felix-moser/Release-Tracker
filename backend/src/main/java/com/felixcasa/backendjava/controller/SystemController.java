package com.felixcasa.backendjava.controller;

import com.felixcasa.backendjava.request.SecretsRequest;
import com.felixcasa.backendjava.response.SecretsResponse;
import com.felixcasa.backendjava.service.SystemService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService){
        this.systemService = systemService;
    }

    @GetMapping("/api/onboarded")
    public Map<String, Boolean> checkOnboardedController(){
        // Return if system is onboarded
        return systemService.checkOnboarded();
    }

    @GetMapping("/api/secrets")
    public SecretsResponse getSecretsController(){
        //Return which secrets are configured. Sensitive values are masked.
        return systemService.getSecrets();
    }

    @PostMapping("/api/secrets")
    public Map<String, String> saveSecretsController(@Valid @RequestBody SecretsRequest request){
        // Saves Secret and returns saved message
       return systemService.saveSecrets(request);
    }

}

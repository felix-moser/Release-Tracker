package com.felixcasa.backendjava.controller;

import com.felixcasa.backendjava.request.UpdateSchedulerIntervalRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

    private final com.felixcasa.backendjava.service.SchedulerService schedulerService;
    private final com.felixcasa.backendjava.service.SystemService systemService;

    public SchedulerController(com.felixcasa.backendjava.service.SchedulerService schedulerService, com.felixcasa.backendjava.service.SystemService systemService) {
        this.schedulerService = schedulerService;
        this.systemService = systemService;
    }

    @GetMapping("/status")
    public Map<String, Object> getSchedulerStatus(){
        return Map.of(
            "running", schedulerService.isRunning(),
            "interval_minutes", schedulerService.getIntervalMinutes()
        );
    }

    @PutMapping("/interval")
    public Map<String, Object> updateSchedulerInterval(@Valid @RequestBody UpdateSchedulerIntervalRequest intervalRequest){
        if (intervalRequest.intervalMinutes() < 1) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "interval_minutes must be at least 1.");
        }
        
        systemService.setSecret("CHECK_INTERVAL_MINUTES", String.valueOf(intervalRequest.intervalMinutes()));
        
        return Map.of(
            "running", schedulerService.isRunning(),
            "interval_minutes", intervalRequest.intervalMinutes()
        );
    }
}

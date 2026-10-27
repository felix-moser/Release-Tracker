package com.felixcasa.backendjava.controller;
import com.felixcasa.backendjava.service.DockerImageService;
import com.felixcasa.backendjava.request.DockerImageRequest;
import com.felixcasa.backendjava.response.DockerImageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController
@RequestMapping("/api/docker-images")
public class DockerImageController {
    private final DockerImageService service;
    public DockerImageController(DockerImageService service) { this.service=service; }
    @GetMapping public List<DockerImageResponse> list() { return service.list(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public DockerImageResponse create(@Valid @RequestBody DockerImageRequest request) { return service.save(null,request); }
    @PutMapping public DockerImageResponse update(@RequestParam("image_name") String original, @Valid @RequestBody DockerImageRequest request) {
        return service.save(original,request);
    }
    @DeleteMapping public Map<String,String> delete(@RequestParam("image_name") String image) {
        service.delete(image); return Map.of("message", "Docker image removed");
    }
}

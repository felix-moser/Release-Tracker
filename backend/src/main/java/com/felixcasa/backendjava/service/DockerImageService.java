package com.felixcasa.backendjava.service;
import com.felixcasa.backendjava.client.DockerRegistryClient;
import com.felixcasa.backendjava.repository.DockerImageRepository;
import com.felixcasa.backendjava.request.DockerImageRequest;
import com.felixcasa.backendjava.response.DockerImageResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
@Service
public class DockerImageService {
    private final DockerImageRepository repository;
    private final DockerRegistryClient client;
    public DockerImageService(DockerImageRepository repository, DockerRegistryClient client) {
        this.repository=repository; this.client=client;
    }
    public List<DockerImageResponse> list() { return repository.listStandalone(); }
    public DockerImageResponse save(String original, DockerImageRequest request) {
        if (original != null && list().stream().noneMatch(i -> i.imageName().equals(original)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Standalone Docker image not found");
        String image = DockerRegistryClient.normalizeImage(request.imageName());
        if (!image.equals(original) && repository.exists(image))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Docker image already exists");
        String digest = client.fetchLatestDigest(image);
        var trackedImage = new DockerImageRepository.TrackedImage(image, digest, request.templateName());
        if (original == null) repository.insert(trackedImage, null);
        else if (!repository.updateStandalone(original, trackedImage))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Standalone Docker image not found");
        return new DockerImageResponse(image,digest,request.templateName());
    }
    public void delete(String image) {
        if (!repository.deleteStandalone(image)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Standalone Docker image not found");
    }
}

package com.felixcasa.backendjava.repository;
import com.felixcasa.backendjava.response.DockerImageResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class DockerImageRepository {
    private final JdbcClient jdbc;
    public DockerImageRepository(JdbcClient jdbc) { this.jdbc = jdbc; }
    public List<DockerImageResponse> listStandalone() {
        return jdbc.sql("SELECT image_name,manifest_digest,template_name FROM docker_images WHERE repo_name IS NULL ORDER BY image_name")
            .query(DockerImageResponse.class).list();
    }
    public boolean exists(String image) {
        return jdbc.sql("SELECT count(*) FROM docker_images WHERE image_name=?").param(image).query(Integer.class).single() > 0;
    }
    public record TrackedImage(String imageName, String manifestDigest, String templateName) {}

    public void insert(TrackedImage image, String repo) {
        jdbc.sql("INSERT INTO docker_images(image_name,manifest_digest,template_name,repo_name) VALUES (:image,:digest,:template,:repo)")
            .param("image", image.imageName()).param("digest", image.manifestDigest()).param("template", image.templateName()).param("repo", repo).update();
    }
    public boolean updateStandalone(String original, TrackedImage image) {
        return jdbc.sql("UPDATE docker_images SET image_name=:image,manifest_digest=:digest,template_name=:template WHERE image_name=:original AND repo_name IS NULL")
            .param("image",image.imageName()).param("digest",image.manifestDigest()).param("template",image.templateName()).param("original",original).update() > 0;
    }
    public boolean deleteStandalone(String image) {
        return jdbc.sql("DELETE FROM docker_images WHERE image_name=? AND repo_name IS NULL").param(image).update() > 0;
    }
    public void deleteLinked(String repo) { jdbc.sql("DELETE FROM docker_images WHERE repo_name=?").param(repo).update(); }
    public void writeDigest(String image, String digest) {
        jdbc.sql("UPDATE docker_images SET manifest_digest=? WHERE image_name=?").params(digest,image).update();
    }
}

package com.felixcasa.backendjava.repository;

import com.felixcasa.backendjava.response.RepositoryResponse;
import com.felixcasa.backendjava.model.VersionComparisonMode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Repository
public class RepoRepository {

    private static final RowMapper<RepositoryResponse> REPOSITORY_ROW_MAPPER = (rs, rowNum) ->
            new RepositoryResponse(
                    rs.getString("repo"),
                    rs.getString("repo_url"),
                    rs.getString("latest_tag"),
                    rs.getString("template_name"),
                    VersionComparisonMode.valueOf(rs.getString("version_comparison")),
                    rs.getString("image_name"),
                    rs.getString("manifest_digest"),
                    rs.getString("docker_template"));

    private final JdbcClient jdbcClient;

    public RepoRepository(JdbcClient jdbcClient){
        this.jdbcClient = jdbcClient;
    }

    public Optional<String> getLatestWrittenRelease(String repoName){
        return this.jdbcClient
                .sql("SELECT latest_tag FROM releases WHERE repo = :repo")
                .param("repo", repoName)
                .query(String.class)
                .optional();
    }

    public void writeLatestRelease(String repoName, String latestTag){
        if (latestTag == null || latestTag.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not determine latest tag for repository");
        }

        this.jdbcClient
                .sql("UPDATE releases SET latest_tag = :tag WHERE repo = :repo")
                .param("tag", latestTag)
                .param("repo", repoName)
                .update();
    }

    public boolean addNewRepo(String repoName, String repoUrl, String tag, String templateName, VersionComparisonMode versionComparison){
        try {
            if ((this.jdbcClient.sql("SELECT 1 FROM releases WHERE repo = :repo OR repo_url = :repoUrl LIMIT 1").param("repo", repoName).param("repoUrl", repoUrl).query(String.class).optional()).isPresent()) {
                return false;
            } else {
                // IF repo name/url is not used create Repo
                this.jdbcClient
                        .sql("INSERT INTO releases (repo, repo_url, latest_tag, template_name, version_comparison) VALUES (:repo, :repoUrl, :tag, :template, :mode)")
                        .param("repo", repoName)
                        .param("repoUrl", repoUrl)
                        .param("tag", tag)
                        .param("template", templateName)
                        .param("mode", versionComparison.name())
                        .update();
                return true;
            }
        } catch (DataIntegrityViolationException dataIntegrityViolationException){
            return false;
        }
    }

    public boolean updateRepo(String originalName, String newName, String repoUrl, String tag, String templateName, VersionComparisonMode versionComparison){
        try {

            int rowsAffected = this.jdbcClient
                    .sql("UPDATE releases SET repo = :newRepo, repo_url = :newUrl, latest_tag = :newTag, template_name = :newTemplate, version_comparison = :mode WHERE repo = :oldRepo")
                    .param("newRepo", newName)
                    .param("newUrl", repoUrl)
                    .param("newTag", tag)
                    .param("newTemplate", templateName)
                    .param("mode", versionComparison.name())
                    .param("oldRepo", originalName)
                    .update();
            return rowsAffected > 0;
        }catch (DataIntegrityViolationException dataIntegrityViolationException){
            return false;
        }
    }

    public List<RepositoryResponse> listRepos(){
        return this.jdbcClient
                .sql("SELECT r.*, d.image_name, d.manifest_digest, d.template_name AS docker_template FROM releases r LEFT JOIN docker_images d ON d.repo_name=r.repo ORDER BY r.repo")
                .query(REPOSITORY_ROW_MAPPER)
                .list();
    }

    public boolean removeRepo(String repoName){
        int rowsAffected = this.jdbcClient
                .sql("DELETE FROM releases WHERE repo = :repo")
                .param("repo", repoName)
                .update();

        return rowsAffected > 0;
    }


    public Optional<RepositoryResponse> listRepo(String repoName) {
        return this.jdbcClient
                .sql("SELECT r.*, d.image_name, d.manifest_digest, d.template_name AS docker_template FROM releases r LEFT JOIN docker_images d ON d.repo_name=r.repo WHERE r.repo = :repo")
                .param("repo", repoName)
                .query(REPOSITORY_ROW_MAPPER)
                .optional();
    }
}

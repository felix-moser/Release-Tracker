package com.felixcasa.backendjava.repository;

import com.felixcasa.backendjava.response.RepositoryResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Repository
public class RepoRepository {

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

    public boolean addNewRepo(String repoName, String repoUrl, String tag, String templateName){
        try {
            if ((this.jdbcClient.sql("SELECT 1 FROM releases WHERE repo = :repo OR repo_url = :repoUrl LIMIT 1").param("repo", repoName).param("repoUrl", repoUrl).query(String.class).optional()).isPresent()) {
                return false;
            } else {
                // IF repo name/url is not used create Repo
                this.jdbcClient
                        .sql("INSERT INTO releases (repo, repo_url, latest_tag, template_name) VALUES  (:repo, :repoUrl, :tag, :template)")
                        .param("repo", repoName)
                        .param("repoUrl", repoUrl)
                        .param("tag", tag)
                        .param("template", templateName)
                        .update();
                return true;
            }
        } catch (DataIntegrityViolationException dataIntegrityViolationException){
            return false;
        }
    }

    public boolean updateRepo(String originalName, String newName, String repoUrl, String tag, String templateName){
        try {

            int rowsAffected = this.jdbcClient
                    .sql("UPDATE releases SET repo = :newRepo, repo_url = :newUrl, latest_tag = :newTag, template_name = :newTemplate WHERE repo = :oldRepo")
                    .param("newRepo", newName)
                    .param("newUrl", repoUrl)
                    .param("newTag", tag)
                    .param("newTemplate", templateName)
                    .param("oldRepo", originalName)
                    .update();
            return rowsAffected > 0;
        }catch (DataIntegrityViolationException dataIntegrityViolationException){
            return false;
        }
    }

    public List<RepositoryResponse> listRepos(){
        return this.jdbcClient
                .sql("SELECT repo, repo_url, latest_tag, template_name FROM releases ORDER BY repo")
                .query((rs, rowNum) ->
                        new RepositoryResponse(
                                rs.getString("repo"),
                                rs.getString("repo_url"),
                                rs.getString("latest_tag"),
                                rs.getString("template_name")
                        )
                )
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
                .sql("SELECT repo, repo_url, latest_tag, template_name FROM releases WHERE repo = :repo")
                .param("repo", repoName)
                .query((rs, rowNum) ->
                        new RepositoryResponse(
                                rs.getString("repo"),
                                rs.getString("repo_url"),
                                rs.getString("latest_tag"),
                                rs.getString("template_name")
                        )
                )
                .optional();
    }
}

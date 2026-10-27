package com.felixcasa.backendjava;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.nio.file.Files;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DockerTrackingIntegrationTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.felixcasa.backendjava.client.DockerRegistryClient registry;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.felixcasa.backendjava.client.GithubClient github;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.felixcasa.backendjava.client.JiraClient jira;
    @Autowired JdbcClient jdbc;
    @LocalServerPort int port;
    final java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
    java.net.http.HttpResponse<String> request(String method, String path, String body) throws Exception {
        return http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + path))
            .header("Content-Type", "application/json").method(method, java.net.http.HttpRequest.BodyPublishers.ofString(body)).build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    }
    @Test void registryCredentialsAreNormalizedRedactedAndRetainedWhenTokenOmitted() throws Exception {
        assertThat(request("POST", "/api/system/registries", "{\"registry\":\"ghcr.io\",\"username\":\"alice\"}").statusCode()).isEqualTo(400);
        assertThat(request("POST", "/api/system/registries", "{\"registry\":\"index.docker.io\",\"username\":\"alice\",\"token\":\"private-token\"}").statusCode()).isEqualTo(200);
        assertThat(request("POST", "/api/system/registries", "{\"registry\":\"docker.io\",\"username\":\"bob\"}").statusCode()).isEqualTo(200);
        var response = request("GET", "/api/system/registries", "");
        assertThat(response.body()).contains("docker.io", "bob", "token_set").doesNotContain("private-token");
        assertThat(jdbc.sql("SELECT token FROM registry_credentials WHERE registry='docker.io'").query(String.class).single()).isEqualTo("private-token");
        assertThat(request("DELETE", "/api/system/registries?registry=hub.docker.com", "").statusCode()).isEqualTo(200);
        assertThat(request("GET", "/api/system/registries", "").body()).isEqualTo("[]");
    }

    static final String database = database();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", () -> database);
    }
    static String database() {
        try { return "jdbc:sqlite:" + Files.createTempFile("docker-tracking-", ".db"); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
    @Test void standaloneCrudNeverListsOrModifiesLinkedImages() throws Exception {
        org.mockito.Mockito.when(registry.fetchLatestDigest(org.mockito.ArgumentMatchers.anyString())).thenReturn("sha256:" + "a".repeat(64));
        jdbc.sql("INSERT INTO releases(repo,repo_url,latest_tag) VALUES ('linked','linked-url','v1')").update();
        jdbc.sql("INSERT INTO docker_images VALUES ('linked/app','sha256:old','docker','linked')").update();
        assertThat(request("POST", "/api/docker-images", "{\"image\":\"nginx:old\",\"template\":\"docker\"}").statusCode()).isEqualTo(201);
        assertThat(request("GET", "/api/docker-images", "").body()).contains("nginx", "docker").doesNotContain("linked/app");
        assertThat(request("POST", "/api/docker-images", "{\"image_name\":\"linked/app\"}").statusCode()).isEqualTo(409);
        assertThat(request("PUT", "/api/docker-images?image_name=linked%2Fapp", "{\"image_name\":\"other\"}").statusCode()).isEqualTo(404);
        assertThat(request("DELETE", "/api/docker-images?image_name=linked%2Fapp", "").statusCode()).isEqualTo(404);
        assertThat(request("PUT", "/api/docker-images?image_name=nginx", "{\"image_name\":\"redis:old\",\"template_name\":\"new\"}").statusCode()).isEqualTo(200);
        assertThat(request("GET", "/api/docker-images", "").body()).contains("redis", "new").doesNotContain("nginx");
        assertThat(request("DELETE", "/api/docker-images?image_name=redis", "").statusCode()).isEqualTo(200);
        assertThat(request("GET", "/api/docker-images", "").body()).isEqualTo("[]");
    }
    @Test void repositorySettingsManageLinkedImageAndRollbackOnConflicts() throws Exception {
        org.mockito.Mockito.when(github.getRepository(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.nullable(String.class))).thenReturn("v1");
        org.mockito.Mockito.when(registry.fetchLatestDigest(org.mockito.ArgumentMatchers.anyString())).thenReturn("sha256:" + "a".repeat(64));
        String body = "{\"name\":\"app\",\"github_url\":\"https://github.com/owner/app\",\"docker_image\":\"nginx:old\",\"docker_template\":\"docker\"}";
        var created = request("POST", "/api/repos", body);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("\"docker_image\":\"nginx\"", "\"docker_template\":\"docker\"", "docker_digest");
        assertThat(request("GET", "/api/docker-images", "").body()).isEqualTo("[]");
        assertThat(request("PUT", "/api/repos/app", body.replace("\"app\"", "\"renamed\"")).statusCode()).isEqualTo(200);
        assertThat(jdbc.sql("SELECT repo_name FROM docker_images").query(String.class).single()).isEqualTo("renamed");
        assertThat(request("POST", "/api/docker-images", "{\"image\":\"redis\"}").statusCode()).isEqualTo(201);
        var conflict = request("PUT", "/api/repos/renamed", body.replace("\"app\"", "\"renamed\"").replace("nginx:old", "redis"));
        assertThat(conflict.statusCode()).isEqualTo(409);
        assertThat(request("GET", "/api/repos", "").body()).contains("nginx");
        assertThat(request("PUT", "/api/repos/renamed", body.replace("\"app\"", "\"renamed\"").replace("nginx:old", "")).statusCode()).isEqualTo(200);
        assertThat(jdbc.sql("SELECT count(*) FROM docker_images WHERE repo_name IS NOT NULL").query(Integer.class).single()).isZero();
        assertThat(request("PUT", "/api/repos/renamed", body.replace("\"app\"", "\"renamed\"")).statusCode()).isEqualTo(200);
        assertThat(request("DELETE", "/api/repos/renamed", "").statusCode()).isEqualTo(200);
        assertThat(jdbc.sql("SELECT count(*) FROM docker_images WHERE repo_name IS NOT NULL").query(Integer.class).single()).isZero();
    }
    @Test void duplicateStandaloneImageReturnsConflictWithoutDependingOnRegistryAvailability() throws Exception {
        jdbc.sql("INSERT INTO docker_images VALUES ('nginx','sha256:old',NULL,NULL)").update();
        org.mockito.Mockito.when(registry.fetchLatestDigest("nginx")).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY));
        assertThat(request("POST", "/api/docker-images", "{\"image\":\"nginx:old\"}").statusCode()).isEqualTo(409);
        org.mockito.Mockito.verifyNoInteractions(registry);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"v2,b,true,github", "v2,a,true,github", "v1,b,true,docker", "v1,a,false,none"})
    void checkEndpointPersistsOnlyProcessedChanges(String tag, String digestHex, boolean updated, String ticketTemplate) throws Exception {
        String oldDigest = "sha256:" + "a".repeat(64);
        String newDigest = "sha256:" + digestHex.repeat(64);
        jdbc.sql("INSERT INTO releases(repo,repo_url,latest_tag,template_name) VALUES ('app','url','v1','github')").update();
        jdbc.sql("INSERT INTO docker_images VALUES ('nginx',?,'docker','app')").param(oldDigest).update();
        for (String template : java.util.List.of("github","docker")) {
            jdbc.sql("INSERT INTO ticket_templates(name,project_key,issue_type,summary,description) VALUES (?,'TEST','Task',?,?)")
                .params(template,template + "|{repo}|{tag}|{url}|{image}|{digest}","{repo}|{tag}|{url}|{image}|{digest}").update();
        }
        jdbc.sql("INSERT INTO secrets VALUES ('JIRA_URL','https://jira.example'),('JIRA_EMAIL','alice'),('JIRA_API_TOKEN','token')").update();
        org.mockito.Mockito.when(github.getRepository(org.mockito.ArgumentMatchers.eq("url"),org.mockito.ArgumentMatchers.nullable(String.class))).thenReturn(tag);
        org.mockito.Mockito.when(registry.fetchLatestDigest("nginx")).thenReturn(newDigest);
        org.mockito.Mockito.when(jira.createIssue(org.mockito.ArgumentMatchers.anyMap(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString())).thenReturn("TEST-1");
        var response = request("POST", "/api/repos/check", "");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"checked\":1", "\"updated\":" + updated);
        assertThat(jdbc.sql("SELECT latest_tag FROM releases").query(String.class).single()).isEqualTo(tag);
        assertThat(jdbc.sql("SELECT manifest_digest FROM docker_images").query(String.class).single()).isEqualTo(newDigest);
        if (updated) {
            var payload = org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
            org.mockito.Mockito.verify(jira).createIssue(payload.capture(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString());
            var fields=(java.util.Map<?,?>) payload.getValue().get("fields");
            assertThat(fields.get("summary")).isEqualTo(ticketTemplate.equals("github") ? "github|app|v2|url| | " : "docker| |latest| |nginx|bbbbbbbbbbbb");
        } else org.mockito.Mockito.verifyNoInteractions(jira);
    }

    @Test void standaloneCheckEndpointUsesItsOwnTemplateAndRecordsDigest() throws Exception {
        jdbc.sql("INSERT INTO docker_images VALUES ('redis','sha256:old','standalone',NULL)").update();
        jdbc.sql("INSERT INTO ticket_templates(name,project_key,issue_type,summary) VALUES ('standalone','TEST','Task','{image}|{tag}|{digest}|{repo}|{url}')").update();
        jdbc.sql("INSERT INTO secrets VALUES ('JIRA_URL','https://jira.example'),('JIRA_EMAIL','alice'),('JIRA_API_TOKEN','token')").update();
        org.mockito.Mockito.when(registry.fetchLatestDigest("redis")).thenReturn("sha256:" + "c".repeat(64));
        org.mockito.Mockito.when(jira.createIssue(org.mockito.ArgumentMatchers.anyMap(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString())).thenReturn("TEST-2");
        var response=request("POST", "/api/repos/check", "");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("redis", "TEST-2", "\"checked\":1");
        var payload=org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
        org.mockito.Mockito.verify(jira).createIssue(payload.capture(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString());
        assertThat(((java.util.Map<?,?>) payload.getValue().get("fields")).get("summary")).isEqualTo("redis|latest|cccccccccccc| | ");
        assertThat(jdbc.sql("SELECT manifest_digest FROM docker_images").query(String.class).single()).isEqualTo("sha256:" + "c".repeat(64));
    }
    @BeforeEach void reset() {
        jdbc.sql("DELETE FROM docker_images").update();
        jdbc.sql("DELETE FROM releases").update();
        jdbc.sql("DELETE FROM registry_credentials").update();
        jdbc.sql("DELETE FROM ticket_templates").update();
        jdbc.sql("DELETE FROM secrets").update();
    }
    @Test void linkedImagesCascadeOnRepositoryRenameAndDelete() {
        assertThat(jdbc.sql("PRAGMA foreign_keys").query(Integer.class).single()).isEqualTo(1);
        jdbc.sql("INSERT INTO releases(repo,repo_url,latest_tag) VALUES ('app','url','v1')").update();
        jdbc.sql("INSERT INTO docker_images VALUES ('nginx','sha256:abc','docker','app')").update();
        jdbc.sql("UPDATE releases SET repo='renamed' WHERE repo='app'").update();
        assertThat(jdbc.sql("SELECT repo_name FROM docker_images").query(String.class).single()).isEqualTo("renamed");
        jdbc.sql("DELETE FROM releases WHERE repo='renamed'").update();
        assertThat(jdbc.sql("SELECT count(*) FROM docker_images").query(Integer.class).single()).isZero();
    }
}
